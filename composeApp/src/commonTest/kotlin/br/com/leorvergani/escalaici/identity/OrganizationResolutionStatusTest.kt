package br.com.leorvergani.escalaici.identity

import kotlin.test.Test
import kotlin.test.assertEquals

class OrganizationResolutionStatusTest {

    @Test
    fun toStatusMapsTerminalResultGroups() {
        assertEquals(OrganizationResolutionStatus.TEAM_FOUND, resolved().toStatus())
        assertEquals(OrganizationResolutionStatus.MEMBER_FOUND, noActiveTeam().toStatus())
        assertEquals(OrganizationResolutionStatus.MEMBER_NOT_FOUND, OrganizationResolutionResult.MemberNotFound("a", "b").toStatus())
        assertEquals(OrganizationResolutionStatus.MEMBER_NOT_FOUND, OrganizationResolutionResult.MemberInactive("member").toStatus())
        assertEquals(OrganizationResolutionStatus.MEMBER_NOT_FOUND, OrganizationResolutionResult.MemberIdentityAmbiguous(listOf("a", "b")).toStatus())
        assertEquals(OrganizationResolutionStatus.TEAM_NOT_FOUND, OrganizationResolutionResult.TeamNotFound("member", "team").toStatus())
        assertEquals(OrganizationResolutionStatus.MULTIPLE_ACTIVE_TEAMS, OrganizationResolutionResult.MultipleActiveTeams("member", listOf("a", "b")).toStatus())
        assertEquals(OrganizationResolutionStatus.ERROR, OrganizationResolutionResult.MembershipNotFound("member").toStatus())
        assertEquals(OrganizationResolutionStatus.ERROR, OrganizationResolutionResult.WorkspaceMismatch("ici", "demo-v1").toStatus())
        assertEquals(OrganizationResolutionStatus.ERROR, OrganizationResolutionResult.DataSourceUnavailable("missing").toStatus())
    }

    private fun resolved() = OrganizationResolutionResult.Resolved(context())

    private fun noActiveTeam() = OrganizationResolutionResult.MemberFoundNoActiveTeam(
        context(primaryTeamId = null, primaryTeamName = null)
    )

    private fun context(
        primaryTeamId: String? = "team",
        primaryTeamName: String? = "Team"
    ) = ResolvedOrganizationContext(
        workspaceId = "ici",
        identitySource = IdentitySource.CORPORATE_MSAL,
        memberId = "member",
        memberDisplayName = "Member",
        normalizedLogin = "member",
        normalizedEmail = "member@example.invalid",
        primaryTeamId = primaryTeamId,
        primaryTeamName = primaryTeamName,
        roleDisplayName = null,
        activeMemberships = emptyList()
    )
}
