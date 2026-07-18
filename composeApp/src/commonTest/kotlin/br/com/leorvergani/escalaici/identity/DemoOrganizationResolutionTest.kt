package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.platform.TodayProvider
import br.com.leorvergani.escalaici.repository.TeamRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DemoOrganizationResolutionTest {

    @Test
    fun demoAnalystPersonasResolveExpectedMemberAndWorkspace() = runTest {
        val resolver = demoResolver()
        val soc = resolver.resolveDemoPersona(DemoPersonaCatalog.personas[0])
        val security = resolver.resolveDemoPersona(DemoPersonaCatalog.personas[1])

        assertTrue(soc is OrganizationResolutionResult.Resolved)
        assertEquals("member-demo-soc-01", soc.context.memberId)
        assertEquals("demo-v1", soc.context.workspaceId)
        assertEquals("team-demo-soc", soc.context.primaryTeamId)

        assertTrue(security is OrganizationResolutionResult.Resolved)
        assertEquals("member-demo-seguranca-01", security.context.memberId)
        assertEquals("demo-v1", security.context.workspaceId)
        assertEquals("team-demo-seguranca", security.context.primaryTeamId)
    }

    @Test
    fun demoManagerReturnsMultipleActiveTeamsWithInjectedFixtureData() = runTest {
        val result = demoResolver().resolveDemoPersona(DemoPersonaCatalog.personas[2])

        assertEquals(
            OrganizationResolutionResult.MultipleActiveTeams(
                memberId = "member-demo-gestor-seguranca",
                teamIds = listOf("team-demo-soc", "team-demo-seguranca")
            ),
            result
        )
    }

    @Test
    fun corporateResolutionUsesCorporateWorkspace() = runTest {
        val member = Member(
            email = "corp@example.invalid",
            scaleName = "corp.login",
            displayName = "Pessoa Corp",
            id = "corp-member"
        )
        val resolver = DefaultOrganizationIdentityResolver(
            corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(
                members = listOf(member),
                loginByMemberId = mapOf("corp-member" to "corp.login")
            ),
            corporateMembershipRepository = InMemoryMembershipRepository(
                listOf(
                    br.com.leorvergani.escalaici.model.MemberTeamMembership(
                        id = "corp-membership",
                        memberId = "corp-member",
                        teamId = "corp-team",
                        startDate = "2020-01-01"
                    )
                )
            ),
            corporateMemberRepository = InMemoryMemberRepository(listOf(member)),
            corporateTeamRepository = InMemoryTeamRepository(
                listOf(br.com.leorvergani.escalaici.model.Team(teamId = "corp-team", name = "Corp Team"))
            ),
            todayProvider = TodayProvider { br.com.leorvergani.escalaici.model.LabDate(2026, 7, 18) }
        )

        val result = resolver.resolveCorporateIdentity(
            CorporateIdentity(
                tenantId = "tenant",
                objectId = "object",
                username = "corp.login",
                displayName = "Pessoa Corp",
                email = "corp@example.invalid",
                accountId = "account"
            )
        )

        assertTrue(result is OrganizationResolutionResult.Resolved)
        assertEquals("ici", result.context.workspaceId)
        assertEquals(IdentitySource.CORPORATE_MSAL, result.context.identitySource)
    }

    @Test
    fun corporateMemberNotFoundDoesNotQueryDemoTeams() = runTest {
        val demoTeams = CountingTeamRepository()
        val resolver = DefaultOrganizationIdentityResolver(
            corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(emptyList()),
            corporateMembershipRepository = InMemoryMembershipRepository(emptyList()),
            corporateMemberRepository = InMemoryMemberRepository(emptyList()),
            corporateTeamRepository = InMemoryTeamRepository(emptyList()),
            demoTeamRepository = demoTeams
        )

        val result = resolver.resolveCorporateIdentity(
            CorporateIdentity(
                tenantId = "tenant",
                objectId = "object",
                username = "missing",
                displayName = "Missing",
                email = "missing@example.invalid",
                accountId = "account"
            )
        )

        assertTrue(result is OrganizationResolutionResult.MemberNotFound)
        assertEquals(0, demoTeams.calls)
    }

    private fun demoResolver(pkg: DemoFixturePackage = demoResolutionFixturePackage()) = DefaultOrganizationIdentityResolver(
        corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(emptyList()),
        corporateMembershipRepository = InMemoryMembershipRepository(emptyList()),
        corporateMemberRepository = InMemoryMemberRepository(emptyList()),
        corporateTeamRepository = InMemoryTeamRepository(emptyList()),
        demoMemberDirectoryRepository = InMemoryMemberDirectoryRepository(
            members = pkg.toMembers(),
            workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
            loginByMemberId = pkg.loginByMemberId()
        ),
        demoMembershipRepository = InMemoryMembershipRepository(pkg.toMemberships()),
        demoMemberRepository = InMemoryMemberRepository(pkg.toMembers()),
        demoTeamRepository = InMemoryTeamRepository(pkg.toTeams()),
        todayProvider = TodayProvider { br.com.leorvergani.escalaici.model.LabDate(2026, 7, 18) }
    )

    private fun demoResolutionFixturePackage(): DemoFixturePackage {
        val personas = DemoPersonaCatalog.personas.associateBy { it.memberId }
        return DemoFixturePackage(
            schemaVersion = 1,
            workspace = DemoFixtureWorkspace(
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                workspaceType = "DEMO",
                scenarioId = "resolution-test",
                seedVersion = 1,
                publicationRevision = 1,
                externalEffectsAllowed = false,
                notificationsEnabled = false
            ),
            teams = listOf(
                DemoFixtureTeam(
                    id = "team-demo-soc",
                    workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                    name = "SOC Demonstracao",
                    acronym = "SOC-DEMO",
                    active = true,
                    schemaVersion = 1
                ),
                DemoFixtureTeam(
                    id = "team-demo-seguranca",
                    workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                    name = "Seguranca da Informacao Demonstracao",
                    acronym = "SEG-DEMO",
                    active = true,
                    schemaVersion = 1
                )
            ),
            members = listOf(
                demoFixtureMember(personas.getValue("member-demo-soc-01")),
                demoFixtureMember(personas.getValue("member-demo-seguranca-01")),
                demoFixtureMember(personas.getValue("member-demo-gestor-seguranca"))
            ),
            memberTeamMemberships = listOf(
                demoFixtureMembership(
                    id = "membership-demo-soc-01",
                    memberId = "member-demo-soc-01",
                    teamId = "team-demo-soc",
                    isPrimary = true
                ),
                demoFixtureMembership(
                    id = "membership-demo-seguranca-01",
                    memberId = "member-demo-seguranca-01",
                    teamId = "team-demo-seguranca",
                    isPrimary = true
                ),
                demoFixtureMembership(
                    id = "membership-demo-gestor-soc",
                    memberId = "member-demo-gestor-seguranca",
                    teamId = "team-demo-soc",
                    isPrimary = false
                ),
                demoFixtureMembership(
                    id = "membership-demo-gestor-seguranca",
                    memberId = "member-demo-gestor-seguranca",
                    teamId = "team-demo-seguranca",
                    isPrimary = false
                )
            ),
            teamManagerAssignments = emptyList(),
            scheduleChangeRequests = emptyList(),
            schedulePeriods = emptyList(),
            scheduleAssignments = emptyList(),
            publicationRecords = emptyList()
        )
    }

    private fun demoFixtureMember(persona: DemoPersona) = DemoFixtureMember(
        id = persona.memberId,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        displayName = persona.displayName,
        corporateLogin = persona.fictitiousLogin,
        emailNormalized = persona.fictitiousEmail,
        active = true,
        schemaVersion = 1
    )

    private fun demoFixtureMembership(
        id: String,
        memberId: String,
        teamId: String,
        isPrimary: Boolean
    ) = DemoFixtureMembership(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        memberId = memberId,
        teamId = teamId,
        startDate = "2020-01-01",
        endDate = null,
        active = true,
        isPrimary = isPrimary,
        schemaVersion = 1
    )

    private class CountingTeamRepository : TeamRepository {
        var calls = 0

        override suspend fun getTeam(teamId: String): br.com.leorvergani.escalaici.model.Team? {
            calls += 1
            return null
        }

        override suspend fun getTeams(): List<br.com.leorvergani.escalaici.model.Team> {
            calls += 1
            return emptyList()
        }
    }
}
