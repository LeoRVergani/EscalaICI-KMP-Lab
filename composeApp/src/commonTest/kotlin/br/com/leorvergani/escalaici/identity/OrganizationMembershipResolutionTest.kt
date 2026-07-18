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

class OrganizationMembershipResolutionTest {

    @Test
    fun activeMembershipResolvesTeamContext() = runTest {
        val result = resolver(listOf(membership())).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("team-1", result.context.primaryTeamId)
        assertEquals(1, result.context.activeMemberships.size)
    }

    @Test
    fun inactiveMembershipReturnsMemberFoundNoActiveTeam() = runTest {
        val result = resolver(listOf(membership(active = false))).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.MemberFoundNoActiveTeam)
        assertEquals(null, result.context.primaryTeamId)
        assertEquals(emptyList(), result.context.activeMemberships)
    }

    @Test
    fun futureMembershipReturnsMemberFoundNoActiveTeam() = runTest {
        val result = resolver(listOf(membership(startDate = "2026-07-19"))).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.MemberFoundNoActiveTeam)
    }

    @Test
    fun expiredMembershipReturnsMemberFoundNoActiveTeam() = runTest {
        val result = resolver(listOf(membership(endDate = "2026-07-17"))).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.MemberFoundNoActiveTeam)
    }

    @Test
    fun missingMembershipsReturnMembershipNotFound() = runTest {
        val result = resolver(emptyList()).resolveCorporateIdentity(identity())

        assertEquals(OrganizationResolutionResult.MembershipNotFound("member-1"), result)
    }

    @Test
    fun multipleActiveMembershipsWithOnePrimaryResolveThatPrimary() = runTest {
        val result = resolver(
            memberships = listOf(
                membership(teamId = "team-1", isPrimary = false),
                membership(teamId = "team-2", isPrimary = true)
            ),
            teams = listOf(team("team-1", "Team 1"), team("team-2", "Team 2"))
        ).resolveCorporateIdentity(identity())

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("team-2", result.context.primaryTeamId)
        assertEquals(listOf("team-1", "team-2"), result.context.activeMemberships.map { it.teamId })
    }

    @Test
    fun multipleActiveMembershipsWithoutPrimaryReturnMultipleActiveTeams() = runTest {
        val result = resolver(
            memberships = listOf(
                membership(teamId = "team-1", isPrimary = false),
                membership(teamId = "team-2", isPrimary = false)
            ),
            teams = listOf(team("team-1", "Team 1"), team("team-2", "Team 2"))
        ).resolveCorporateIdentity(identity())

        assertEquals(
            OrganizationResolutionResult.MultipleActiveTeams("member-1", listOf("team-1", "team-2")),
            result
        )
    }

    private fun resolver(
        memberships: List<MemberTeamMembership>,
        teams: List<Team> = listOf(team())
    ) = DefaultOrganizationIdentityResolver(
        corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(
            members = listOf(member()),
            loginByMemberId = mapOf("member-1" to "ana.login")
        ),
        corporateMembershipRepository = InMemoryMembershipRepository(memberships),
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

    private fun member() = Member(
        email = "ana@example.invalid",
        scaleName = "ana.login",
        displayName = "Ana",
        id = "member-1"
    )

    private fun membership(
        teamId: String = "team-1",
        active: Boolean = true,
        startDate: String = "2020-01-01",
        endDate: String? = null,
        isPrimary: Boolean = false
    ) = MemberTeamMembership(
        id = "membership-$teamId",
        memberId = "member-1",
        teamId = teamId,
        startDate = startDate,
        endDate = endDate,
        active = active,
        isPrimary = isPrimary
    )

    private fun team(teamId: String = "team-1", name: String = "Team 1") =
        Team(teamId = teamId, name = name, displayName = name)
}
