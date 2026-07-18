package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.platform.SystemTodayProvider
import br.com.leorvergani.escalaici.platform.TodayProvider
import br.com.leorvergani.escalaici.repository.MemberRepository
import br.com.leorvergani.escalaici.repository.TeamRepository

interface OrganizationIdentityResolver {
    suspend fun resolveCorporateIdentity(identity: CorporateIdentity): OrganizationResolutionResult
    suspend fun resolveDemoPersona(persona: DemoPersona): OrganizationResolutionResult
}

class DefaultOrganizationIdentityResolver(
    private val corporateMemberDirectoryRepository: MemberDirectoryRepository,
    private val corporateMembershipRepository: MembershipRepository = InMemoryMembershipRepository(),
    private val corporateMemberRepository: MemberRepository,
    private val corporateTeamRepository: TeamRepository,
    private val demoMemberDirectoryRepository: MemberDirectoryRepository = DemoMemberDirectoryRepository(),
    private val demoMembershipRepository: MembershipRepository = DemoMembershipRepository(),
    private val demoMemberRepository: MemberRepository = DemoMemberRepository(),
    private val demoTeamRepository: TeamRepository = DemoTeamRepository(),
    private val todayProvider: TodayProvider = SystemTodayProvider
) : OrganizationIdentityResolver {

    override suspend fun resolveCorporateIdentity(identity: CorporateIdentity): OrganizationResolutionResult {
        val normalizedEmail = normalizeIdentity(identity.email)
        val normalizedLogin = normalizeIdentity(identity.username)
        if (normalizedEmail == null && normalizedLogin == null) {
            return OrganizationResolutionResult.DataSourceUnavailable(
                "Corporate identity has no email or username to resolve."
            )
        }

        return resolve(
            workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
            identitySource = IdentitySource.CORPORATE_MSAL,
            normalizedEmail = normalizedEmail,
            normalizedLogin = normalizedLogin,
            memberDirectoryRepository = corporateMemberDirectoryRepository,
            membershipRepository = corporateMembershipRepository,
            memberRepository = corporateMemberRepository,
            teamRepository = corporateTeamRepository
        )
    }

    override suspend fun resolveDemoPersona(persona: DemoPersona): OrganizationResolutionResult =
        resolve(
            workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
            identitySource = IdentitySource.DEMO_PERSONA,
            normalizedEmail = normalizeIdentity(persona.fictitiousEmail),
            normalizedLogin = normalizeIdentity(persona.fictitiousLogin),
            memberDirectoryRepository = demoMemberDirectoryRepository,
            membershipRepository = demoMembershipRepository,
            memberRepository = demoMemberRepository,
            teamRepository = demoTeamRepository
        )

    private suspend fun resolve(
        workspaceId: String,
        identitySource: IdentitySource,
        normalizedEmail: String?,
        normalizedLogin: String?,
        memberDirectoryRepository: MemberDirectoryRepository,
        membershipRepository: MembershipRepository,
        memberRepository: MemberRepository,
        teamRepository: TeamRepository
    ): OrganizationResolutionResult {
        val candidateIds = memberDirectoryRepository
            .findActiveMemberIds(normalizedEmail, normalizedLogin)
            .distinct()

        if (candidateIds.isEmpty()) {
            return OrganizationResolutionResult.MemberNotFound(normalizedEmail, normalizedLogin)
        }
        if (candidateIds.size > 1) {
            return OrganizationResolutionResult.MemberIdentityAmbiguous(candidateIds)
        }

        val memberId = candidateIds.single()
        val member = memberRepository.getMember(memberId)
            ?: return OrganizationResolutionResult.MemberNotFound(normalizedEmail, normalizedLogin)

        member.workspaceMismatch(workspaceId)?.let { return it }
        if (!member.active) return OrganizationResolutionResult.MemberInactive(memberId)

        val memberships = membershipRepository.getMemberships(memberId)
        if (memberships.isEmpty()) return OrganizationResolutionResult.MembershipNotFound(memberId)
        memberships.firstNotNullOfOrNull { it.workspaceMismatch(workspaceId) }?.let { return it }

        val today = todayProvider.today()
        val activeMemberships = memberships.filter { it.isActiveOn(today) }
        if (activeMemberships.isEmpty()) {
            return OrganizationResolutionResult.MemberFoundNoActiveTeam(
                ResolvedOrganizationContext(
                    workspaceId = workspaceId,
                    identitySource = identitySource,
                    memberId = member.id,
                    memberDisplayName = member.displayName,
                    normalizedLogin = normalizedLogin,
                    normalizedEmail = normalizedEmail,
                    primaryTeamId = null,
                    primaryTeamName = null,
                    roleDisplayName = null,
                    activeMemberships = emptyList()
                )
            )
        }

        val primaryMembership = selectPrimaryMembership(memberId, activeMemberships)
        if (primaryMembership is PrimarySelection.Failed) return primaryMembership.result

        val primary = (primaryMembership as PrimarySelection.Selected).membership
        val primaryRoleDisplayName = roleDisplayNameFor(primary)
        val activeTeams = activeMemberships.map { membership ->
            val team = teamRepository.getTeam(membership.teamId)
                ?: return OrganizationResolutionResult.TeamNotFound(memberId, membership.teamId)
            team.workspaceMismatch(workspaceId)?.let { return it }
            membership to team
        }
        val primaryTeam = activeTeams.first { it.first.id == primary.id }.second

        return OrganizationResolutionResult.Resolved(
            ResolvedOrganizationContext(
                workspaceId = workspaceId,
                identitySource = identitySource,
                memberId = member.id,
                memberDisplayName = member.displayName,
                normalizedLogin = normalizedLogin,
                normalizedEmail = normalizedEmail,
                primaryTeamId = primary.teamId,
                primaryTeamName = primaryTeam.displayName,
                roleDisplayName = primaryRoleDisplayName,
                activeMemberships = activeTeams.map { (membership, team) ->
                    ResolvedTeamMembership(
                        teamId = membership.teamId,
                        teamName = team.displayName,
                        roleDisplayName = roleDisplayNameFor(membership),
                        isPrimary = membership.id == primary.id
                    )
                }
            )
        )
    }

    private fun selectPrimaryMembership(
        memberId: String,
        memberships: List<MemberTeamMembership>
    ): PrimarySelection {
        if (memberships.size == 1) return PrimarySelection.Selected(memberships.single())
        val primaryMemberships = memberships.filter { it.isPrimary }
        return if (primaryMemberships.size == 1) {
            PrimarySelection.Selected(primaryMemberships.single())
        } else {
            PrimarySelection.Failed(
                OrganizationResolutionResult.MultipleActiveTeams(
                    memberId = memberId,
                    teamIds = memberships.map { it.teamId }.distinct()
                )
            )
        }
    }

    private fun MemberTeamMembership.isActiveOn(today: LabDate): Boolean {
        if (!active) return false
        val start = LabDate.parseIso(startDate) ?: return false
        val end = endDate?.let { LabDate.parseIso(it) }
        return start <= today && (end == null || end >= today)
    }

    private fun Member.workspaceMismatch(expected: String): OrganizationResolutionResult.WorkspaceMismatch? =
        workspaceId
            ?.takeIf { it != expected }
            ?.let { OrganizationResolutionResult.WorkspaceMismatch(expected, it) }

    private fun Team.workspaceMismatch(expected: String): OrganizationResolutionResult.WorkspaceMismatch? =
        workspaceId
            ?.takeIf { it != expected }
            ?.let { OrganizationResolutionResult.WorkspaceMismatch(expected, it) }

    private fun MemberTeamMembership.workspaceMismatch(expected: String): OrganizationResolutionResult.WorkspaceMismatch? =
        workspaceId
            ?.takeIf { it != expected }
            ?.let { OrganizationResolutionResult.WorkspaceMismatch(expected, it) }

    @Suppress("UNUSED_PARAMETER")
    private fun roleDisplayNameFor(membership: MemberTeamMembership): String? {
        // A fase atual recebe apenas roleId, sem repositorio/catalogo de roles para resolver o rotulo.
        return null
    }

    private sealed interface PrimarySelection {
        data class Selected(val membership: MemberTeamMembership) : PrimarySelection
        data class Failed(val result: OrganizationResolutionResult.MultipleActiveTeams) : PrimarySelection
    }
}
