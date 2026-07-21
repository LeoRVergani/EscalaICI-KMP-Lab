package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.identity.DEMO_DEVELOPER_ROLE
import br.com.leorvergani.escalaici.identity.DefaultOrganizationIdentityResolver
import br.com.leorvergani.escalaici.identity.DemoDataOrigin
import br.com.leorvergani.escalaici.identity.DemoFixtureMember
import br.com.leorvergani.escalaici.identity.DemoFixtureMembership
import br.com.leorvergani.escalaici.identity.DemoFixturePackage
import br.com.leorvergani.escalaici.identity.DemoFixtureScheduleAssignment
import br.com.leorvergani.escalaici.identity.DemoFixtureSchedulePeriod
import br.com.leorvergani.escalaici.identity.DemoFixtureTeam
import br.com.leorvergani.escalaici.identity.DemoFixtureWorkspace
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.DemoPersonaCatalog
import br.com.leorvergani.escalaici.identity.DemoPublicationRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.InMemoryMemberRepository
import br.com.leorvergani.escalaici.identity.InMemoryMembershipRepository
import br.com.leorvergani.escalaici.identity.InMemoryTeamRepository
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import br.com.leorvergani.escalaici.identity.OrganizationWorkspace
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberDirectoryRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMemberRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoMembershipRepository
import br.com.leorvergani.escalaici.identity.RemoteFirstDemoTeamRepository
import br.com.leorvergani.escalaici.identity.scheduleSummaryForMember
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.mockScheduleSummary
import br.com.leorvergani.escalaici.platform.TodayProvider
import br.com.leorvergani.escalaici.source.DemoPublicationGateway
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject

class DemoWorkspaceOverviewDecisionTest {
    @Test
    fun authorizedDemoEntryShowsWorkspaceOverviewWithoutSelectingPersona() = runTest {
        val overview = demoRepository().workspaceOverview()
        val decision = decideDemoWorkspaceEntry(lverganiIdentity(), authorized = true, overview = overview)

        val showOverview = assertIs<DemoWorkspaceEntryDecision.ShowOverview>(decision)
        assertEquals(DEMO_DEVELOPER_ROLE, showOverview.session.role)
        assertEquals("lvergani", showOverview.session.authenticatedLogin)
        assertEquals("demo-v1", showOverview.session.overview.workspaceId)
        assertEquals(DemoDataOrigin.LOCAL_FIXTURE, showOverview.session.overview.state.origin)
        assertEquals(
            listOf("member-demo-soc-01", "member-demo-seguranca-01"),
            showOverview.session.overview.operationalPersonas.map { it.memberId }
        )
    }

    @Test
    fun choosingOperationalPersonaWithAssignmentsResolvesPublishedScheduleWithoutMockFallback() = runTest {
        val repository = demoRepository()
        val persona = repository.workspaceOverview().operationalPersonas.first()
        val resolver = demoResolver(repository)

        val resolution = assertIs<OrganizationResolutionResult.Resolved>(resolver.resolveDemoPersona(persona))
        val publishedSummary = repository.scheduleSummaryForMember(resolution.context.memberId)
        val decision = decideResolvedScheduleSummary(
            currentSummary = mockScheduleSummary(),
            resolvedContext = resolution.context,
            loadPublishedScheduleSummaryAvailable = true,
            publishedSummary = publishedSummary
        )

        val summary = assertNotNull(decision.summary)
        assertNull(decision.errorMessage)
        assertEquals(persona.memberId, summary.member.id)
        assertEquals("team-demo-soc", summary.team.teamId)
        assertEquals(2, summary.days.size)
        assertNotEquals(mockScheduleSummary().member.id, summary.member.id)
        assertNotEquals(mockScheduleSummary().team.teamId, summary.team.teamId)
    }

    @Test
    fun lverganiDeveloperIdentityIsNotTreatedAsAnyDemoPersona() = runTest {
        val overview = demoRepository().workspaceOverview()
        val decision = assertIs<DemoWorkspaceEntryDecision.ShowOverview>(
            decideDemoWorkspaceEntry(lverganiIdentity(), authorized = true, overview = overview)
        )

        assertEquals(DEMO_DEVELOPER_ROLE, decision.session.role)
        assertEquals("lvergani", decision.session.authenticatedLogin)
        assertFalse(decision.session.overview.operationalPersonas.any { it.memberId == "member-demo-gestor-seguranca" })
        assertFalse(decision.session.overview.operationalPersonas.any { it.fictitiousLogin == decision.session.authenticatedLogin })
    }

    @Test
    fun demoOverviewBackReturnsToEntryGate() {
        assertEquals(
            DemoBackNavigationTarget.ENTRY_GATE,
            decideDemoBackNavigation(
                requestedEntryIsDemo = true,
                hasDemoWorkspaceSession = true,
                hasSelectedDemoPersona = false,
                hasSessionMemberId = false
            )
        )
    }

    @Test
    fun demoPersonaBackReturnsToAdministrativeOverview() {
        assertEquals(
            DemoBackNavigationTarget.DEMO_WORKSPACE_OVERVIEW,
            decideDemoBackNavigation(
                requestedEntryIsDemo = true,
                hasDemoWorkspaceSession = true,
                hasSelectedDemoPersona = true,
                hasSessionMemberId = true
            )
        )
    }

    private fun demoRepository(revision: Int = 12) = DemoPublicationRepository(
        resolver = DemoPublicationResolver(UnavailableDemoGateway()),
        fixtureProvider = { demoOverviewFixture(revision) }
    )

    private fun demoResolver(repository: DemoPublicationRepository) = DefaultOrganizationIdentityResolver(
        corporateMemberDirectoryRepository = InMemoryMemberDirectoryRepository(emptyList()),
        corporateMembershipRepository = InMemoryMembershipRepository(emptyList()),
        corporateMemberRepository = InMemoryMemberRepository(emptyList()),
        corporateTeamRepository = InMemoryTeamRepository(emptyList()),
        demoMemberDirectoryRepository = RemoteFirstDemoMemberDirectoryRepository(repository),
        demoMembershipRepository = RemoteFirstDemoMembershipRepository(repository),
        demoMemberRepository = RemoteFirstDemoMemberRepository(repository),
        demoTeamRepository = RemoteFirstDemoTeamRepository(repository),
        demoDataSourceStateProvider = { repository.state() },
        todayProvider = TodayProvider { LabDate(2026, 7, 18) }
    )

    private fun demoOverviewFixture(revision: Int): DemoFixturePackage {
        val personas = DemoPersonaCatalog.personas.associateBy { it.memberId }
        return DemoFixturePackage(
            schemaVersion = 1,
            workspace = DemoFixtureWorkspace(
                workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                workspaceType = "DEMO",
                scenarioId = "overview-test",
                seedVersion = 1,
                publicationRevision = revision,
                externalEffectsAllowed = false,
                notificationsEnabled = false
            ),
            teams = listOf(
                fixtureTeam("team-demo-soc", "SOC Demo"),
                fixtureTeam("team-demo-seguranca", "Segurança Demo")
            ),
            members = listOf(
                fixtureMember(personas.getValue("member-demo-soc-01")),
                fixtureMember(personas.getValue("member-demo-seguranca-01")),
                fixtureMember(personas.getValue("member-demo-gestor-seguranca"))
            ),
            memberTeamMemberships = listOf(
                fixtureMembership("membership-demo-soc", "member-demo-soc-01", "team-demo-soc", isPrimary = true),
                fixtureMembership("membership-demo-seguranca", "member-demo-seguranca-01", "team-demo-seguranca", isPrimary = true),
                fixtureMembership("membership-demo-gestor-soc", "member-demo-gestor-seguranca", "team-demo-soc", isPrimary = false),
                fixtureMembership("membership-demo-gestor-seguranca", "member-demo-gestor-seguranca", "team-demo-seguranca", isPrimary = false)
            ),
            teamManagerAssignments = emptyList(),
            scheduleChangeRequests = emptyList(),
            schedulePeriods = listOf(
                fixturePeriod("period-demo-soc", "team-demo-soc", revision),
                fixturePeriod("period-demo-seguranca", "team-demo-seguranca", revision)
            ),
            scheduleAssignments = listOf(
                fixtureAssignment("assignment-demo-soc-01", "period-demo-soc", "team-demo-soc", "member-demo-soc-01", "2026-07-18"),
                fixtureAssignment("assignment-demo-soc-02", "period-demo-soc", "team-demo-soc", "member-demo-soc-01", "2026-07-19"),
                fixtureAssignment("assignment-demo-seguranca-01", "period-demo-seguranca", "team-demo-seguranca", "member-demo-seguranca-01", "2026-07-18")
            ),
            publicationRecords = emptyList()
        )
    }

    private fun fixtureTeam(id: String, name: String) = DemoFixtureTeam(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        name = name,
        acronym = id.substringAfterLast("-").uppercase(),
        active = true,
        schemaVersion = 1
    )

    private fun fixtureMember(persona: DemoPersona) = DemoFixtureMember(
        id = persona.memberId,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        displayName = persona.displayName,
        corporateLogin = persona.fictitiousLogin,
        emailNormalized = persona.fictitiousEmail,
        active = true,
        schemaVersion = 1
    )

    private fun fixtureMembership(
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

    private fun fixturePeriod(id: String, teamId: String, revision: Int) = DemoFixtureSchedulePeriod(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        teamId = teamId,
        name = "Julho 2026",
        startDate = "2026-07-01",
        endDate = "2026-07-31",
        active = true,
        publicationRevision = revision,
        schemaVersion = 1
    )

    private fun fixtureAssignment(
        id: String,
        periodId: String,
        teamId: String,
        memberId: String,
        date: String
    ) = DemoFixtureScheduleAssignment(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        periodId = periodId,
        teamId = teamId,
        memberId = memberId,
        date = date,
        assignmentType = "WORK_SHIFT",
        shiftName = "manha",
        startTime = "07:00",
        endTime = "13:00",
        schemaVersion = 1
    )

    private fun lverganiIdentity() = CorporateIdentity(
        tenantId = "tenant",
        objectId = "object",
        username = "lvergani",
        displayName = "Leandro Vergani",
        email = "lvergani@ici.tec.br",
        accountId = "account"
    )

    private class UnavailableDemoGateway : DemoPublicationGateway {
        override suspend fun loadDocumentFields(path: String): JsonObject {
            error("404 Not Found")
        }

        override suspend fun loadCollectionDocuments(path: String): List<JsonObject> = emptyList()
    }
}
