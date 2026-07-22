package br.com.leorvergani.escalaici.source

import br.com.leorvergani.escalaici.model.AssignmentSource
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.ScheduleChangeRequest
import br.com.leorvergani.escalaici.model.SchedulePeriod
import br.com.leorvergani.escalaici.model.ScheduleSourceType
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.model.TeamManagerAssignment
import br.com.leorvergani.escalaici.model.TeamManagerPermissions
import br.com.leorvergani.escalaici.model.TeamManagerRole
import br.com.leorvergani.escalaici.model.WorkspacePublicationPointer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object DemoPublicationCollections {
    const val TEAMS = "teams"
    const val MEMBERS = "members"
    const val MEMBERSHIPS = "member_team_memberships"
    const val MANAGERS = "team_manager_assignments"
    const val PERIODS = "schedule_periods"
    const val ASSIGNMENTS = "schedule_assignments"
    const val REQUESTS = "schedule_change_requests"

    val all = listOf(TEAMS, MEMBERS, MEMBERSHIPS, MANAGERS, PERIODS, ASSIGNMENTS, REQUESTS)
}

data class DemoPublicationSnapshot(
    val pointer: WorkspacePublicationPointer,
    val teams: List<Team>,
    val members: List<Member>,
    val memberships: List<MemberTeamMembership>,
    val teamManagerAssignments: List<TeamManagerAssignment>,
    val schedulePeriods: List<SchedulePeriod>,
    val scheduleAssignments: List<ScheduleAssignment>,
    val scheduleChangeRequests: List<ScheduleChangeRequest>
)

internal fun JsonObject.toWorkspacePointer(workspaceIdFromPath: String): WorkspacePublicationPointer {
    val workspaceId = string("workspaceId") ?: string("id") ?: workspaceIdFromPath
    val revision = int("publicationRevision") ?: error("Ponteiro Demo sem publicationRevision inteiro.")
    return WorkspacePublicationPointer(
        workspaceId = workspaceId,
        activeRevision = revision,
        status = string("status"),
        workspaceType = string("workspaceType"),
        scenarioId = string("scenarioId"),
        seedVersion = int("seedVersion"),
        allowedDeveloperObjectIds = stringArray("allowedDeveloperObjectIds")
    )
}

internal fun JsonObject.toDemoTeam(revision: Int) = Team(
    teamId = string("id") ?: string("teamId") ?: error("Time Demo sem id."),
    name = string("name") ?: string("teamName") ?: error("Time Demo sem nome."),
    displayName = string("name") ?: string("teamName") ?: error("Time Demo sem nome."),
    workspaceId = requiredWorkspaceId(),
    publicationRevision = requiredPublicationRevision(revision)
)

internal fun JsonObject.toDemoMember(revision: Int) = Member(
    email = string("emailNormalized") ?: string("email") ?: "",
    scaleName = string("displayName") ?: error("Membro Demo sem displayName."),
    displayName = string("displayName") ?: error("Membro Demo sem displayName."),
    id = string("id") ?: string("memberId") ?: error("Membro Demo sem id."),
    active = bool("active", true),
    workspaceId = requiredWorkspaceId(),
    publicationRevision = requiredPublicationRevision(revision),
    entraTenantId = string("entraTenantId"),
    entraObjectId = string("entraObjectId"),
    // Escrito pelo Dashboard em toda publicacao oficial (server/domain/officialPublicationPlanner.mjs
    // via DemoMemberDto.corporateLogin), mas nunca lido aqui ate esta correcao - a resolucao de
    // identidade por login sempre caia silenciosamente para comparar contra scaleName/displayName
    // em vez do login corporativo real (auditoria de contrato FASE 14f-3).
    corporateLogin = string("corporateLogin")
)

internal fun JsonObject.toDemoMembership(revision: Int) = MemberTeamMembership(
    id = string("id") ?: error("Membership Demo sem id."),
    memberId = string("memberId") ?: error("Membership Demo sem memberId."),
    teamId = string("teamId") ?: error("Membership Demo sem teamId."),
    roleId = string("roleId"),
    startDate = string("startDate") ?: error("Membership Demo sem startDate."),
    endDate = string("endDate"),
    active = bool("active", true),
    isPrimary = bool("isPrimary", false),
    workspaceId = requiredWorkspaceId(),
    publicationRevision = requiredPublicationRevision(revision)
)

internal fun JsonObject.toDemoManagerAssignment(revision: Int) = TeamManagerAssignment(
    id = string("id") ?: error("Vinculo de gestor Demo sem id."),
    workspaceId = requiredWorkspaceId(),
    publicationRevision = requiredPublicationRevision(revision),
    teamId = string("teamId") ?: error("Vinculo de gestor Demo sem teamId."),
    memberId = string("managerMemberId") ?: string("memberId") ?: error("Vinculo de gestor Demo sem managerMemberId."),
    role = when (string("role")) {
        "PRIMARY_MANAGER" -> TeamManagerRole.PRIMARY_MANAGER
        "PRIMARY_APPROVER" -> TeamManagerRole.PRIMARY_APPROVER
        else -> TeamManagerRole.OTHER
    },
    permissions = objectValue("permissions")?.let { permissions ->
        TeamManagerPermissions(
            viewTeamSchedule = permissions.bool("viewTeamSchedule"),
            viewTeamMembers = permissions.bool("viewTeamMembers"),
            editTeamSchedule = permissions.bool("editTeamSchedule"),
            approveScheduleChanges = permissions.bool("approveScheduleChanges"),
            publishSchedule = permissions.bool("publishSchedule"),
            manageTeamAssignments = permissions.bool("manageTeamAssignments")
        )
    } ?: TeamManagerPermissions(),
    active = bool("active", true),
    validFrom = string("validFrom") ?: error("Vinculo de gestor Demo sem validFrom."),
    validTo = string("validTo"),
    createdAt = string("createdAt").orEmpty(),
    updatedAt = string("updatedAt").orEmpty(),
    createdBy = string("createdBy").orEmpty()
)

internal fun JsonObject.toDemoSchedulePeriod(revision: Int) = SchedulePeriod(
    id = string("id") ?: string("periodId") ?: error("Periodo Demo sem id."),
    teamId = string("teamId") ?: error("Periodo Demo sem teamId."),
    startDate = string("startDate") ?: error("Periodo Demo sem startDate."),
    endDate = string("endDate") ?: error("Periodo Demo sem endDate."),
    source = ScheduleSourceType.DEMO,
    updatedAt = string("updatedAt") ?: string("publishedAt") ?: "",
    publicationRevision = requiredPublicationRevision(revision)
)

internal fun JsonObject.toDemoScheduleAssignment(revision: Int) = ScheduleAssignment(
    id = string("id") ?: string("assignmentId") ?: error("Assignment Demo sem id."),
    periodId = string("periodId") ?: error("Assignment Demo sem periodId."),
    teamId = string("teamId") ?: error("Assignment Demo sem teamId."),
    memberId = string("memberId") ?: error("Assignment Demo sem memberId."),
    memberName = string("memberName") ?: string("scaleName") ?: string("memberId") ?: "",
    date = string("date") ?: error("Assignment Demo sem date."),
    shiftType = shiftTypeFrom(string("assignmentType"), string("shiftName")),
    startTime = string("startTime") ?: string("startDateTime")?.substringAfter('T')?.take(5),
    endTime = string("endTime") ?: string("endDateTime")?.substringAfter('T')?.take(5),
    source = AssignmentSource.FIREBASE_SYNC,
    notes = string("note") ?: string("notes"),
    publicationRevision = requiredPublicationRevision(revision)
)

internal fun JsonObject.toDemoChangeRequest(revision: Int) = ScheduleChangeRequest(
    id = string("id") ?: error("Solicitacao Demo sem id."),
    workspaceId = requiredWorkspaceId(),
    publicationRevision = requiredPublicationRevision(revision),
    memberId = string("requesterMemberId") ?: string("memberId") ?: error("Solicitacao Demo sem requesterMemberId."),
    teamId = string("requesterTeamId") ?: string("teamId") ?: error("Solicitacao Demo sem requesterTeamId."),
    periodId = string("schedulePeriodId") ?: string("periodId") ?: error("Solicitacao Demo sem schedulePeriodId."),
    assignmentId = string("assignmentId"),
    status = string("status") ?: error("Solicitacao Demo sem status."),
    assignedManagerMemberId = string("assignedManagerMemberId") ?: error("Solicitacao Demo sem gestor atribuido."),
    requestType = string("requestType") ?: "",
    reason = string("reason") ?: "",
    createdAt = string("createdAt") ?: "",
    resolvedAt = string("resolvedAt"),
    resolvedByMemberId = string("resolvedByMemberId"),
    resolutionNote = string("resolutionNote")
)

internal fun JsonObject.requiredWorkspaceId(): String =
    string("workspaceId") ?: error("Entidade Demo sem workspaceId.")

internal fun JsonObject.requiredPublicationRevision(expected: Int): Int =
    int("publicationRevision") ?: error("Entidade Demo sem publicationRevision inteiro.")

internal fun JsonObject.string(name: String): String? =
    value(this[name], "stringValue") ?: value(this[name], "timestampValue")

internal fun JsonObject.int(name: String): Int? =
    this[name]?.jsonObject?.let { value ->
        value["integerValue"]?.jsonPrimitive?.content?.toIntOrNull()
            ?: value["doubleValue"]?.jsonPrimitive?.intOrNull
    }

internal fun JsonObject.bool(name: String, default: Boolean = false): Boolean =
    this[name]?.jsonObject?.get("booleanValue")?.jsonPrimitive?.booleanOrNull ?: default

internal fun JsonObject.objectValue(name: String): JsonObject? =
    this[name]?.jsonObject?.get("mapValue")?.jsonObject?.get("fields")?.jsonObject

internal fun JsonObject.stringArray(name: String): List<String> =
    this[name]
        ?.jsonObject
        ?.get("arrayValue")
        ?.jsonObject
        ?.get("values")
        ?.jsonArray
        ?.mapNotNull { value(it, "stringValue") }
        ?: emptyList()

private fun value(element: JsonElement?, key: String): String? =
    element?.jsonObject?.get(key)?.jsonPrimitive?.content

private fun shiftTypeFrom(assignmentType: String?, shiftName: String?): ShiftType =
    shiftTypeFromAssignment(assignmentType, shiftName) ?: ShiftType.INDEFINIDO
