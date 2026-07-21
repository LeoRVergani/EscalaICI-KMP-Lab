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

class OrganizationMemberResolutionTest {

    @Test
    fun corporateIdentity_resolvesMemberByEmail() = runTest {
        val resolver = resolver(
            members = listOf(member(id = "member-1", email = "ana@example.invalid", login = "ana.login")),
            memberships = listOf(membership(memberId = "member-1")),
            teams = listOf(team())
        )

        val result = resolver.resolveCorporateIdentity(identity(email = " ANA@example.invalid ", username = "other"))

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("member-1", result.context.memberId)
        assertEquals("ici-dev", result.context.workspaceId)
    }

    @Test
    fun corporateIdentity_resolvesMemberByLogin() = runTest {
        val resolver = resolver(
            members = listOf(member(id = "member-1", email = "ana@example.invalid", login = "ana.login")),
            memberships = listOf(membership(memberId = "member-1")),
            teams = listOf(team())
        )

        val result = resolver.resolveCorporateIdentity(identity(email = null, username = " ANA.LOGIN "))

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("member-1", result.context.memberId)
    }

    @Test
    fun corporateIdentity_prioritizesTenantAndObjectIdBeforeEmailFallback() = runTest {
        val resolver = resolver(
            members = listOf(
                member(id = "member-email", email = "ana@example.invalid"),
                member(
                    id = "member-entra",
                    email = "outra@example.invalid",
                    entraTenantId = "tenant-1",
                    entraObjectId = "object-1"
                )
            ),
            memberships = listOf(membership(memberId = "member-entra")),
            teams = listOf(team())
        )

        val result = resolver.resolveCorporateIdentity(
            identity(
                email = "ana@example.invalid",
                username = "ana.login",
                tenantId = "tenant-1",
                objectId = "object-1"
            )
        )

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("member-entra", result.context.memberId)
    }

    @Test
    fun corporateIdentity_memberNotFoundDoesNotFallbackToDemoRepositories() = runTest {
        val demoDirectory = CountingMemberDirectoryRepository()
        val resolver = resolver(
            members = emptyList(),
            memberships = emptyList(),
            teams = emptyList(),
            demoDirectory = demoDirectory
        )

        val result = resolver.resolveCorporateIdentity(identity(email = "missing@example.invalid", username = "missing"))

        assertTrue(result is OrganizationResolutionResult.MemberNotFound)
        assertEquals(0, demoDirectory.calls)
    }

    @Test
    fun corporateIdentity_inactiveMemberReturnsMemberInactive() = runTest {
        // Usa o InMemoryMemberDirectoryRepository REAL (default de resolver()), nao um
        // stub artificial - prova que um membro inativo que bate exatamente na
        // identidade ainda vira candidato (achado da revisao independente: a versao
        // anterior filtrava `active` no proprio diretorio, tornando este estado
        // inalcancavel na implementacao de producao).
        val resolver = resolver(
            members = listOf(member(id = "member-1", active = false)),
            memberships = listOf(membership(memberId = "member-1")),
            teams = listOf(team())
        )

        val result = resolver.resolveCorporateIdentity(identity(email = "ana@example.invalid", username = "ana.login"))

        assertEquals(OrganizationResolutionResult.MemberInactive("member-1"), result)
    }

    @Test
    fun corporateIdentity_ambiguousIdentityReturnsAllCandidateIds() = runTest {
        val resolver = resolver(
            members = listOf(
                member(id = "member-1", email = "same@example.invalid"),
                member(id = "member-2", email = "same@example.invalid")
            ),
            memberships = emptyList(),
            teams = emptyList()
        )

        val result = resolver.resolveCorporateIdentity(identity(email = "same@example.invalid", username = "unused"))

        assertEquals(OrganizationResolutionResult.MemberIdentityAmbiguous(listOf("member-1", "member-2")), result)
    }

    @Test
    fun corporateIdentity_workspaceMismatchStopsResolution() = runTest {
        val resolver = resolver(
            memberDirectory = FixedMemberDirectoryRepository("member-1"),
            members = listOf(member(id = "member-1", workspaceId = "demo-v1")),
            memberships = listOf(membership(memberId = "member-1")),
            teams = listOf(team())
        )

        val result = resolver.resolveCorporateIdentity(identity(email = "ana@example.invalid", username = "ana.login"))

        assertEquals(OrganizationResolutionResult.WorkspaceMismatch("ici-dev", "demo-v1"), result)
    }

    private fun resolver(
        members: List<Member>,
        memberships: List<MemberTeamMembership>,
        teams: List<Team>,
        memberDirectory: MemberDirectoryRepository = InMemoryMemberDirectoryRepository(
            members = members,
            loginByMemberId = members.associate { it.id to it.scaleName }
        ),
        demoDirectory: MemberDirectoryRepository = InMemoryMemberDirectoryRepository(emptyList())
    ) = DefaultOrganizationIdentityResolver(
        corporateMemberDirectoryRepository = memberDirectory,
        corporateMembershipRepository = InMemoryMembershipRepository(memberships),
        corporateMemberRepository = InMemoryMemberRepository(members),
        corporateTeamRepository = InMemoryTeamRepository(teams),
        demoMemberDirectoryRepository = demoDirectory,
        todayProvider = TodayProvider { LabDate(2026, 7, 18) }
    )

    private fun identity(
        email: String?,
        username: String,
        tenantId: String = "tenant",
        objectId: String = "object"
    ) = CorporateIdentity(
        tenantId = tenantId,
        objectId = objectId,
        username = username,
        displayName = "Ana",
        email = email,
        accountId = "account"
    )

    private fun member(
        id: String,
        email: String = "ana@example.invalid",
        login: String = "ana.login",
        active: Boolean = true,
        workspaceId: String? = null,
        entraTenantId: String? = null,
        entraObjectId: String? = null
    ) = Member(
        email = email,
        scaleName = login,
        displayName = "Ana",
        id = id,
        active = active,
        workspaceId = workspaceId,
        entraTenantId = entraTenantId,
        entraObjectId = entraObjectId
    )

    private fun membership(memberId: String, teamId: String = "team-1") =
        MemberTeamMembership(id = "membership-$memberId-$teamId", memberId = memberId, teamId = teamId, startDate = "2020-01-01")

    private fun team(teamId: String = "team-1") = Team(teamId = teamId, name = "Team 1")

    private class FixedMemberDirectoryRepository(private val memberId: String) : MemberDirectoryRepository {
        override suspend fun findActiveMemberIds(
            normalizedEmail: String?,
            normalizedLogin: String?,
            entraTenantId: String?,
            entraObjectId: String?
        ) = listOf(memberId)
    }

    private class CountingMemberDirectoryRepository : MemberDirectoryRepository {
        var calls = 0
        override suspend fun findActiveMemberIds(
            normalizedEmail: String?,
            normalizedLogin: String?,
            entraTenantId: String?,
            entraObjectId: String?
        ): List<String> {
            calls += 1
            return emptyList()
        }
    }
}
