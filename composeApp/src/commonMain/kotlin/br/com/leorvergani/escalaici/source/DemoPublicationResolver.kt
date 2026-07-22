package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.identity.OrganizationWorkspace
import br.com.leorvergani.escalaici.model.WorkspacePublicationPointer
import kotlinx.serialization.json.JsonObject

sealed interface DemoPublicationLoadResult {
    data class Success(val snapshot: DemoPublicationSnapshot) : DemoPublicationLoadResult
    data class Failure(
        val cause: ScheduleSyncCause,
        val message: String,
        val retryable: Boolean = true
    ) : DemoPublicationLoadResult
}

class DemoPublicationResolver(
    private val gateway: DemoPublicationGateway,
    private val workspaceId: String = OrganizationWorkspace.DEMO_WORKSPACE_ID
) {
    suspend fun loadActivePointer(): WorkspacePublicationPointer {
        val pointerPath = workspacePath()
        return gateway.loadDocumentFields(pointerPath).toWorkspacePointer(workspaceId).validatedPointer(pointerPath)
    }

    suspend fun loadActiveSnapshot(): DemoPublicationLoadResult {
        var changedOnce = false
        while (true) {
            val attempt = loadOneAttempt()
            when (attempt) {
                is AttemptResult.Success -> return DemoPublicationLoadResult.Success(attempt.snapshot)
                is AttemptResult.PointerChanged -> {
                    if (changedOnce) {
                        return DemoPublicationLoadResult.Failure(
                            cause = ScheduleSyncCause.INVALID_REMOTE_DATA,
                            message = "A publicacao mudou durante a leitura. Tente novamente."
                        )
                    }
                    changedOnce = true
                }
                is AttemptResult.Failure -> return DemoPublicationLoadResult.Failure(
                    cause = attempt.cause,
                    message = attempt.message
                )
            }
        }
    }

    private suspend fun loadOneAttempt(): AttemptResult = try {
        val pointerPath = workspacePath()
        val pointer = loadActivePointer()
        val revision = pointer.activeRevision
        val basePath = revisionPath(revision)
        val collections = DemoPublicationCollections.all.associateWith { collection ->
            gateway.loadCollectionDocuments("$basePath/$collection")
        }
        val snapshot = buildSnapshot(pointer, collections)
        val finalPointer = gateway.loadDocumentFields(pointerPath).toWorkspacePointer(workspaceId).validatedPointer(pointerPath)
        if (finalPointer.activeRevision != revision) AttemptResult.PointerChanged else AttemptResult.Success(snapshot)
    } catch (t: Throwable) {
        AttemptResult.Failure(classifySyncFailure(t), safeMessage(classifySyncFailure(t)))
    }

    private fun WorkspacePublicationPointer.validatedPointer(path: String): WorkspacePublicationPointer {
        if (workspaceId != this@DemoPublicationResolver.workspaceId) {
            error("Ponteiro Demo aponta para workspace divergente.")
        }
        if (activeRevision <= 0) {
            error("Ponteiro sem revisao ativa positiva.")
        }
        if (status != null && status != "ACTIVE" && status != "PUBLISHED") {
            error("Ponteiro com status nao ativo.")
        }
        if (path != workspacePath()) {
            error("Ponteiro lido de caminho inesperado.")
        }
        return this
    }

    private fun buildSnapshot(
        pointer: WorkspacePublicationPointer,
        collections: Map<String, List<JsonObject>>
    ): DemoPublicationSnapshot {
        val revision = pointer.activeRevision
        collections.values.flatten().forEach { entity ->
            require(entity.requiredWorkspaceId() == workspaceId) { "Entidade com workspaceId divergente." }
            require(entity.requiredPublicationRevision(revision) == revision) { "Entidade com publicationRevision divergente." }
        }
        val teams = collections.getValue(DemoPublicationCollections.TEAMS).map { it.toDemoTeam(revision) }
        val members = collections.getValue(DemoPublicationCollections.MEMBERS).map { it.toDemoMember(revision) }
        val memberships = collections.getValue(DemoPublicationCollections.MEMBERSHIPS).map { it.toDemoMembership(revision) }
        val managers = collections.getValue(DemoPublicationCollections.MANAGERS).map { it.toDemoManagerAssignment(revision) }
        val periods = collections.getValue(DemoPublicationCollections.PERIODS).map { it.toDemoSchedulePeriod(revision) }
        val assignments = collections.getValue(DemoPublicationCollections.ASSIGNMENTS).map { it.toDemoScheduleAssignment(revision) }
        val requests = collections.getValue(DemoPublicationCollections.REQUESTS).map { it.toDemoChangeRequest(revision) }

        val workspaceValid = teams.all { it.workspaceId == workspaceId } &&
            members.all { it.workspaceId == workspaceId } &&
            memberships.all { it.workspaceId == workspaceId } &&
            managers.all { it.workspaceId == workspaceId } &&
            requests.all { it.workspaceId == workspaceId }
        require(workspaceValid) { "Entidade com workspaceId divergente." }

        val revisionValid = teams.all { it.publicationRevision == revision } &&
            members.all { it.publicationRevision == revision } &&
            memberships.all { it.publicationRevision == revision } &&
            managers.all { it.publicationRevision == revision } &&
            periods.all { it.publicationRevision == revision } &&
            assignments.all { it.publicationRevision == revision } &&
            requests.all { it.publicationRevision == revision }
        require(revisionValid) { "Entidade com publicationRevision divergente." }

        val teamIds = teams.map { it.teamId }.toSet()
        val memberIds = members.map { it.id }.toSet()
        require(memberships.all { it.memberId in memberIds && it.teamId in teamIds }) {
            "Membership referencia membro ou time ausente."
        }

        return DemoPublicationSnapshot(pointer, teams, members, memberships, managers, periods, assignments, requests)
    }

    private fun workspacePath() = "workspaces/$workspaceId"
    private fun revisionPath(revision: Int) = "${workspacePath()}/revisions/$revision"

    private fun safeMessage(cause: ScheduleSyncCause): String = when (cause) {
        ScheduleSyncCause.AUTH_REQUIRED -> "Publicacao remota indisponivel."
        ScheduleSyncCause.FIRESTORE_DATABASE_DISABLED -> "Banco Firestore remoto indisponivel."
        ScheduleSyncCause.PERMISSION_DENIED -> "Sem permissao para ler a publicacao remota."
        ScheduleSyncCause.NETWORK_ERROR -> "Nao foi possivel conectar ao Firebase."
        ScheduleSyncCause.INVALID_REMOTE_DATA -> "A publicacao remota esta em formato inesperado."
        ScheduleSyncCause.WORKSPACE_NOT_PUBLISHED -> "Nenhuma publicacao existe ainda para este workspace."
        else -> "Nao foi possivel carregar a publicacao remota."
    }

    private sealed interface AttemptResult {
        data class Success(val snapshot: DemoPublicationSnapshot) : AttemptResult
        data object PointerChanged : AttemptResult
        data class Failure(val cause: ScheduleSyncCause, val message: String) : AttemptResult
    }
}
