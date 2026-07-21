package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.Team
import br.com.leorvergani.escalaici.platform.TodayProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class OrganizationTeamResolutionTest {

    @Test
    fun teamFoundReturnsResolvedContext() = runTest {
        val result = resolver(teams = listOf(team())).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("Team 1", result.context.primaryTeamName)
    }

    @Test
    fun missingTeamReturnsTeamNotFound() = runTest {
        val result = resolver(teams = emptyList()).resolveCorporateIdentity(identity())

        assertEquals(OrganizationResolutionResult.TeamNotFound("member-1", "team-1"), result)
    }

    @Test
    fun teamWorkspaceMismatchStopsResolution() = runTest {
        val result = resolver(teams = listOf(team(workspaceId = "demo-v1"))).resolveCorporateIdentity(identity())

        assertEquals(OrganizationResolutionResult.WorkspaceMismatch("ici-dev", "demo-v1"), result)
    }

    @Test
    fun teamInactiveCaseIsNotTestedBecauseTeamModelHasNoActiveField() {
        // Team ainda nao possui campo active; a regra nao existe na camada pura atual.
        assertTrue(true)
    }

    private fun resolver(teams: List<Team>) = DefaultOrganizationIdentityResolver(
        corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(
            members = listOf(member()),
            loginByMemberId = mapOf("member-1" to "ana.login")
        ),
        corporateMembershipRepository = InMemoryMembershipRepository(
            listOf(MemberTeamMembership(id = "membership-1", memberId = "member-1", teamId = "team-1", startDate = "2020-01-01"))
        ),
        corporateMemberRepository = InMemoryMemberRepository(listOf(member())),
        corporateTeamRepository = InMemoryTeamRepository(teams),
        todayProvider = TodayProvider { LabDate(2026, 7, 18) }
    )

    private fun identity() = CorporateIdentity(
        tenantId = "tenant",
        objectId = "object",
        username = "ana.login",
        displayName = "Ana",
        email = "ana@example.invalid",
        accountId = "account"
    )

    private fun member() = Member(email = "ana@example.invalid", scaleName = "ana.login", displayName = "Ana", id = "member-1")

    private fun team(workspaceId: String? = null) =
        Team(teamId = "team-1", name = "Team 1", displayName = "Team 1", workspaceId = workspaceId)
}
