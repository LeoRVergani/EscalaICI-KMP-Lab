package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.SchedulePeriod
import br.com.leorvergani.escalaici.model.ScheduleSummary
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.repository.MemberRepository
import br.com.leorvergani.escalaici.repository.TeamRepository
import br.com.leorvergani.escalaici.source.DemoPublicationLoadResult
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import br.com.leorvergani.escalaici.source.DemoPublicationSnapshot
import br.com.leorvergani.escalaici.source.ScheduleSyncCause
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface MemberDirectoryRepository {
    suspend fun findActiveMemberIds(
        normalizedEmail: String?,
        normalizedLogin: String?,
        entraTenantId: String? = null,
        entraObjectId: String? = null
    ): List<String>
}

interface MembershipRepository {
    suspend fun getMemberships(memberId: String): List<MemberTeamMembership>
}

class InMemoryMemberDirectoryRepository(
    private val members: List<Member>,
    private val workspaceId: String = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
    private val loginByMemberId: Map<String, String> = emptyMap()
) : MemberDirectoryRepository {
    // Nao filtra por `active` aqui de proposito: um membro inativo que bate
    // exatamente na identidade ainda precisa ser retornado como candidato,
    // para que o resolver (nao este diretorio) classifique corretamente como
    // MemberInactive em vez de MemberNotFound (achado da revisao independente
    // desta fase - filtrar aqui tornava MemberInactive inalcancavel).
    override suspend fun findActiveMemberIds(
        normalizedEmail: String?,
        normalizedLogin: String?,
        entraTenantId: String?,
        entraObjectId: String?
    ): List<String> {
        val scopedMembers = members
            .asSequence()
            .filter { it.workspaceId == null || it.workspaceId == workspaceId }
            .toList()

        val entraMatches = if (!entraTenantId.isNullOrBlank() && !entraObjectId.isNullOrBlank()) {
            scopedMembers.filter { member ->
                member.entraTenantId == entraTenantId && member.entraObjectId == entraObjectId
            }
        } else {
            emptyList()
        }
        if (entraMatches.isNotEmpty()) return entraMatches.map { it.id }.distinct()

        if (normalizedEmail == null && normalizedLogin == null) return emptyList()
        return scopedMembers
            .asSequence()
            .filter { member ->
                val memberEmail = normalizeIdentity(member.email)
                val memberLogin = normalizeIdentity(loginByMemberId[member.id] ?: member.scaleName)
                (normalizedEmail != null && memberEmail == normalizedEmail) ||
                    (normalizedLogin != null && memberLogin == normalizedLogin)
            }
            .map { it.id }
            .distinct()
            .toList()
    }
}

class InMemoryMembershipRepository(
    private val memberships: List<MemberTeamMembership> = emptyList()
) : MembershipRepository {
    override suspend fun getMemberships(memberId: String): List<MemberTeamMembership> =
        memberships.filter { it.memberId == memberId }
}

class InMemoryMemberRepository(
    private val members: List<Member>
) : MemberRepository {
    override suspend fun getMember(memberId: String): Member? =
        members.firstOrNull { it.id == memberId }

    override suspend fun getMembersByTeam(teamId: String): List<Member> =
        members.filter { it.teamId == teamId }
}

class InMemoryTeamRepository(
    private val teams: List<Team>
) : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? =
        teams.firstOrNull { it.teamId == teamId }

    override suspend fun getTeams(): List<Team> = teams
}

class DemoMemberDirectoryRepository : MemberDirectoryRepository {
    override suspend fun findActiveMemberIds(
        normalizedEmail: String?,
        normalizedLogin: String?,
        entraTenantId: String?,
        entraObjectId: String?
    ): List<String> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberDirectoryRepository(
            members = pkg.toMembers(),
            workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
            loginByMemberId = pkg.loginByMemberId()
        ).findActiveMemberIds(normalizedEmail, normalizedLogin)
    }
}

class DemoMembershipRepository : MembershipRepository {
    override suspend fun getMemberships(memberId: String): List<MemberTeamMembership> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMembershipRepository(pkg.toMemberships()).getMemberships(memberId)
    }
}

class DemoMemberRepository : MemberRepository {
    override suspend fun getMember(memberId: String): Member? {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberRepository(pkg.toMembers()).getMember(memberId)
    }

    override suspend fun getMembersByTeam(teamId: String): List<Member> {
        val pkg = DemoFixtureCache.get()
        return InMemoryMemberRepository(pkg.toMembers()).getMembersByTeam(teamId)
    }
}

class DemoTeamRepository : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? {
        val pkg = DemoFixtureCache.get()
        return InMemoryTeamRepository(pkg.toTeams()).getTeam(teamId)
    }

    override suspend fun getTeams(): List<Team> {
        val pkg = DemoFixtureCache.get()
        return InMemoryTeamRepository(pkg.toTeams()).getTeams()
    }
}

enum class DemoDataOrigin {
    REMOTE_PUBLICATION,
    LOCAL_FIXTURE,
    REMOTE_UNAVAILABLE
}

data class DemoDataSourceState(
    val origin: DemoDataOrigin,
    val publicationRevision: Int?,
    val fallbackCause: ScheduleSyncCause? = null,
    val message: String? = null,
    val allowedDeveloperObjectIds: List<String> = emptyList()
)

const val DEMO_DEVELOPER_ROLE = "DEMO_DEVELOPER"

data class DemoWorkspaceOverview(
    val workspaceId: String,
    val developerRole: String = DEMO_DEVELOPER_ROLE,
    val state: DemoDataSourceState,
    val teamNames: List<String>,
    val memberCount: Int,
    val activeMemberCount: Int,
    val activePeriodLabel: String?,
    val operationalPersonas: List<DemoPersona>
)

class DemoPublicationRepository(
    private val resolver: DemoPublicationResolver,
    private val fixtureProvider: (suspend () -> DemoFixturePackage)? = { DemoFixtureCache.get() }
) {
    private val mutex = Mutex()
    private var cached: DemoPublicationData? = null

    suspend fun data(): DemoPublicationData = mutex.withLock {
        cached?.let { return@withLock it }
        val loaded = when (val remote = resolver.loadActiveSnapshot()) {
            is DemoPublicationLoadResult.Success -> remote.snapshot.toData()
            is DemoPublicationLoadResult.Failure -> fixtureProvider
                ?.invoke()
                ?.toData(fallbackCause = remote.cause, message = remote.message)
                ?: DemoPublicationData(
                    members = emptyList(),
                    teams = emptyList(),
                    memberships = emptyList(),
                    schedulePeriods = emptyList(),
                    scheduleAssignments = emptyList(),
                    loginByMemberId = emptyMap(),
                    state = DemoDataSourceState(
                        origin = DemoDataOrigin.REMOTE_UNAVAILABLE,
                        publicationRevision = null,
                        fallbackCause = remote.cause,
                        message = remote.message
                    )
                )
        }
        cached = loaded
        loaded
    }

    suspend fun state(): DemoDataSourceState = data().state

    suspend fun workspaceOverview(): DemoWorkspaceOverview = data().toWorkspaceOverview()
}

data class DemoPublicationData(
    val members: List<Member>,
    val teams: List<Team>,
    val memberships: List<MemberTeamMembership>,
    val schedulePeriods: List<SchedulePeriod>,
    val scheduleAssignments: List<ScheduleAssignment>,
    val loginByMemberId: Map<String, String>,
    val state: DemoDataSourceState
)

suspend fun DemoPublicationRepository.scheduleSummaryForMember(memberId: String): ScheduleSummary? {
    val data = data()
    val member = data.members.firstOrNull { it.id == memberId } ?: return null
    val membership = data.memberships.firstOrNull { it.memberId == memberId && it.active } ?: return null
    val team = data.teams.firstOrNull { it.teamId == membership.teamId } ?: return null
    val assignments = data.scheduleAssignments
        .filter { it.memberId == memberId && it.teamId == team.teamId }
        .sortedBy { it.date }
    if (assignments.isEmpty()) return null
    val membersById = data.members.associateBy { it.id }
    val teamAssignmentsByDate = data.scheduleAssignments
        .filter { it.teamId == team.teamId && it.shiftType.isWorkShift }
        .groupBy { it.date }
    val period = data.schedulePeriods.firstOrNull { it.id == assignments.first().periodId }
    return ScheduleSummary(
        member = member,
        team = team,
        days = assignments.map { assignment ->
            val date = LabDate.parseIso(assignment.date)
            val colleagueNames = teamAssignmentsByDate[assignment.date]
                .orEmpty()
                .filter { it.memberId != memberId }
                .mapNotNull { colleague -> membersById[colleague.memberId]?.scaleName }
            val colleagueNamesByShift = teamAssignmentsByDate[assignment.date]
                .orEmpty()
                .filter { it.memberId != memberId }
                .groupBy { it.shiftType }
                .mapValues { (_, values) -> values.mapNotNull { membersById[it.memberId]?.scaleName } }
            ShiftDay(
                dayLabel = date?.dayOfWeekShort() ?: "",
                dateLabel = date?.dateLabel() ?: assignment.date,
                fullDateLabel = date?.fullDateLabel() ?: assignment.date,
                type = assignment.shiftType,
                date = date,
                teamMembers = colleagueNames,
                membersByShift = colleagueNamesByShift,
                note = assignment.notes,
                label = assignment.shiftType.label
            )
        },
        periodLabel = period?.let { "${it.startDate} a ${it.endDate}" } ?: "",
        pauseLabel = "--:--",
        pauseOffsetLabel = "",
        sourceFileName = null,
        remoteSourceLabel = data.state.message?.remoteSourceLabel()?.takeIf { it.isNotBlank() }
    )
}

private fun String.remoteSourceLabel(): String = removePrefix("Fonte:").trim()

fun DemoPublicationData.toWorkspaceOverview(): DemoWorkspaceOverview {
    val memberIdsWithAssignments = scheduleAssignments.map { it.memberId }.toSet()
    val operationalPersonas = DemoPersonaCatalog.personas.filter { it.memberId in memberIdsWithAssignments }
    val activePeriod = schedulePeriods.minByOrNull { it.startDate }

    return DemoWorkspaceOverview(
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        state = state,
        teamNames = teams.map { it.displayName }.sorted(),
        memberCount = members.size,
        activeMemberCount = members.count { it.active },
        activePeriodLabel = activePeriod?.let { "${it.startDate} a ${it.endDate}" },
        operationalPersonas = operationalPersonas
    )
}

class RemoteFirstDemoMemberDirectoryRepository(
    private val publicationRepository: DemoPublicationRepository,
    private val workspaceId: String = OrganizationWorkspace.DEMO_WORKSPACE_ID
) : MemberDirectoryRepository {
    override suspend fun findActiveMemberIds(
        normalizedEmail: String?,
        normalizedLogin: String?,
        entraTenantId: String?,
        entraObjectId: String?
    ): List<String> {
        val data = publicationRepository.data()
        return InMemoryMemberDirectoryRepository(
            members = data.members,
            workspaceId = workspaceId,
            loginByMemberId = data.loginByMemberId
        ).findActiveMemberIds(normalizedEmail, normalizedLogin, entraTenantId, entraObjectId)
    }
}

class RemoteFirstDemoMembershipRepository(
    private val publicationRepository: DemoPublicationRepository
) : MembershipRepository {
    override suspend fun getMemberships(memberId: String): List<MemberTeamMembership> =
        InMemoryMembershipRepository(publicationRepository.data().memberships).getMemberships(memberId)
}

class RemoteFirstDemoMemberRepository(
    private val publicationRepository: DemoPublicationRepository
) : MemberRepository {
    override suspend fun getMember(memberId: String): Member? =
        InMemoryMemberRepository(publicationRepository.data().members).getMember(memberId)

    override suspend fun getMembersByTeam(teamId: String): List<Member> {
        val data = publicationRepository.data()
        val memberIds = data.memberships.filter { it.teamId == teamId && it.active }.map { it.memberId }.toSet()
        return data.members.filter { it.id in memberIds }
    }
}

class RemoteFirstDemoTeamRepository(
    private val publicationRepository: DemoPublicationRepository
) : TeamRepository {
    override suspend fun getTeam(teamId: String): Team? =
        InMemoryTeamRepository(publicationRepository.data().teams).getTeam(teamId)

    override suspend fun getTeams(): List<Team> =
        InMemoryTeamRepository(publicationRepository.data().teams).getTeams()
}

private fun DemoPublicationSnapshot.toData() = DemoPublicationData(
    members = members,
    teams = teams,
    memberships = memberships,
    schedulePeriods = schedulePeriods,
    scheduleAssignments = scheduleAssignments,
    // Antes sempre vazio: a publicacao oficial do Dashboard grava corporateLogin em todo
    // membro, mas essa leitura ignorava o campo - a resolucao por login sempre comparava
    // contra scaleName (ver fallback em findActiveMemberIds). Agora alinhado com o caminho
    // de fixture (DemoFixturePackage.loginByMemberId()), que ja fazia isso corretamente.
    loginByMemberId = members.mapNotNull { member ->
        member.corporateLogin?.takeIf { it.isNotBlank() }?.let { member.id to it }
    }.toMap(),
    state = DemoDataSourceState(
        origin = DemoDataOrigin.REMOTE_PUBLICATION,
        publicationRevision = pointer.activeRevision,
        message = "Fonte: publicacao remota rev. ${pointer.activeRevision}",
        allowedDeveloperObjectIds = pointer.allowedDeveloperObjectIds
    )
)

private fun DemoFixturePackage.toData(
    fallbackCause: ScheduleSyncCause?,
    message: String?
) = DemoPublicationData(
    members = toMembers(),
    teams = toTeams(),
    memberships = toMemberships(),
    schedulePeriods = toSchedulePeriods(),
    scheduleAssignments = toScheduleAssignments(),
    loginByMemberId = loginByMemberId(),
    state = DemoDataSourceState(
        origin = DemoDataOrigin.LOCAL_FIXTURE,
        publicationRevision = workspace.publicationRevision,
        fallbackCause = fallbackCause,
        message = message ?: "Fonte: fixture Demo local rev. ${workspace.publicationRevision}"
    )
)
