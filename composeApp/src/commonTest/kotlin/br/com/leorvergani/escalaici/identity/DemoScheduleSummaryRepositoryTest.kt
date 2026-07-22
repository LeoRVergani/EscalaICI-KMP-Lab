package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.source.DemoPublicationGateway
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject

class DemoScheduleSummaryRepositoryTest {
    @Test
    fun scheduleSummaryIncludesWorkingColleaguesForSameTeamAndDate() = runTest {
        val summary = repositoryWithAssignments(
            fixtureAssignment("assignment-member-1", "member-1", "2026-07-27", "Manhã"),
            fixtureAssignment("assignment-member-2", "member-2", "2026-07-27", "Manhã"),
            fixtureAssignment("assignment-member-3", "member-3", "2026-07-27", "Tarde"),
            fixtureAssignment("assignment-member-2-next-day", "member-2", "2026-07-28", "Manhã")
        ).scheduleSummaryForMember("member-1")

        val day = assertNotNull(summary).days.single()
        assertEquals(ShiftType.MANHA, day.type)
        assertEquals(listOf("Pessoa Dois", "Pessoa Tres"), day.teamMembers)
        assertEquals(
            mapOf(
                ShiftType.MANHA to listOf("Pessoa Dois"),
                ShiftType.TARDE to listOf("Pessoa Tres")
            ),
            day.membersByShift
        )
    }

    @Test
    fun scheduleSummaryExcludesNonWorkingColleaguesFromMembersByShift() = runTest {
        val summary = repositoryWithAssignments(
            fixtureAssignment("assignment-member-1", "member-1", "2026-07-27", "Manhã"),
            fixtureAssignment("assignment-member-2", "member-2", "2026-07-27", "Tarde"),
            fixtureAssignment("assignment-member-3-ferias", "member-3", "2026-07-27", "Férias", assignmentType = "VACATION"),
            fixtureAssignment("assignment-member-4-folga", "member-4", "2026-07-27", null, assignmentType = "OFF"),
            fixtureAssignment("assignment-member-5-afastamento", "member-5", "2026-07-27", null, assignmentType = "ABSENCE")
        ).scheduleSummaryForMember("member-1")

        val day = assertNotNull(summary).days.single()
        assertEquals(mapOf(ShiftType.TARDE to listOf("Pessoa Dois")), day.membersByShift)
        assertFalse(day.membersByShift.values.flatten().any { it in listOf("Pessoa Tres", "Pessoa Quatro", "Pessoa Cinco") })
    }

    @Test
    fun scheduleSummaryKeepsTeamMembersEmptyWhenNobodyElseWorksThatDay() = runTest {
        val summary = repositoryWithAssignments(
            fixtureAssignment("assignment-member-1", "member-1", "2026-07-27", "Comercial"),
            fixtureAssignment("assignment-member-2", "member-2", "2026-07-28", "Comercial")
        ).scheduleSummaryForMember("member-1")

        val day = assertNotNull(summary).days.single()
        assertEquals(ShiftType.COMERCIAL, day.type)
        assertEquals(emptyList(), day.teamMembers)
    }

    @Test
    fun scheduleSummaryCountsCommercialDaysAsWorkedDays() = runTest {
        val summary = repositoryWithAssignments(
            fixtureAssignment("assignment-member-1-27", "member-1", "2026-07-27", "Comercial"),
            fixtureAssignment("assignment-member-1-28", "member-1", "2026-07-28", "COMERCIAL"),
            fixtureAssignment("assignment-member-1-29", "member-1", "2026-07-29", "comercial")
        ).scheduleSummaryForMember("member-1")

        assertNotNull(summary)
        assertEquals(3, summary.workedDays)
        assertEquals(0, summary.restDays)
        assertEquals(18, summary.totalHours)
    }

    @Test
    fun scheduleSummaryStoresDemoPublicationSourceSeparatelyFromImportedFileName() = runTest {
        val summary = repositoryWithAssignments(
            fixtureAssignment("assignment-member-1", "member-1", "2026-07-27", "Comercial")
        ).scheduleSummaryForMember("member-1")

        assertNotNull(summary)
        assertNull(summary.sourceFileName)
        assertEquals("Nao foi possivel carregar a publicacao remota.", summary.remoteSourceLabel)
        assertFalse(summary.remoteSourceLabel.orEmpty().startsWith("Fonte:"))
        assertEquals(false, summary.isImported)
    }

    private fun repositoryWithAssignments(vararg assignments: DemoFixtureScheduleAssignment) = DemoPublicationRepository(
        resolver = DemoPublicationResolver(UnavailableDemoGateway()),
        fixtureProvider = {
            DemoFixturePackage(
                schemaVersion = 1,
                workspace = DemoFixtureWorkspace(
                    workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                    workspaceType = "DEMO",
                    scenarioId = "summary-test",
                    seedVersion = 1,
                    publicationRevision = 3,
                    externalEffectsAllowed = false,
                    notificationsEnabled = false
                ),
                teams = listOf(
                    DemoFixtureTeam(
                        id = "team-demo-summary",
                        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                        name = "Equipe Demo Summary",
                        acronym = "SUM",
                        active = true,
                        schemaVersion = 1
                    )
                ),
                members = listOf(
                    fixtureMember("member-1", "Pessoa Um"),
                    fixtureMember("member-2", "Pessoa Dois"),
                    fixtureMember("member-3", "Pessoa Tres"),
                    fixtureMember("member-4", "Pessoa Quatro"),
                    fixtureMember("member-5", "Pessoa Cinco")
                ),
                memberTeamMemberships = listOf(
                    fixtureMembership("member-1"),
                    fixtureMembership("member-2"),
                    fixtureMembership("member-3"),
                    fixtureMembership("member-4"),
                    fixtureMembership("member-5")
                ),
                teamManagerAssignments = emptyList(),
                scheduleChangeRequests = emptyList(),
                schedulePeriods = listOf(
                    DemoFixtureSchedulePeriod(
                        id = "period-demo-summary",
                        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
                        teamId = "team-demo-summary",
                        name = "Julho 2026",
                        startDate = "2026-07-26",
                        endDate = "2026-08-25",
                        active = true,
                        publicationRevision = 3,
                        schemaVersion = 1
                    )
                ),
                scheduleAssignments = assignments.toList(),
                publicationRecords = emptyList()
            )
        }
    )

    private fun fixtureMember(id: String, displayName: String) = DemoFixtureMember(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        displayName = displayName,
        corporateLogin = id,
        emailNormalized = "$id@example.invalid",
        active = true,
        schemaVersion = 1
    )

    private fun fixtureMembership(memberId: String) = DemoFixtureMembership(
        id = "membership-$memberId",
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        memberId = memberId,
        teamId = "team-demo-summary",
        startDate = "2020-01-01",
        endDate = null,
        active = true,
        isPrimary = true,
        schemaVersion = 1
    )

    private fun fixtureAssignment(
        id: String,
        memberId: String,
        date: String,
        shiftName: String?,
        assignmentType: String = "WORK_SHIFT"
    ) = DemoFixtureScheduleAssignment(
        id = id,
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        periodId = "period-demo-summary",
        teamId = "team-demo-summary",
        memberId = memberId,
        date = date,
        assignmentType = assignmentType,
        shiftName = shiftName,
        startTime = "08:00",
        endTime = "18:00",
        schemaVersion = 1
    )

    private class UnavailableDemoGateway : DemoPublicationGateway {
        override suspend fun loadDocumentFields(path: String): JsonObject {
            error("404 Not Found")
        }

        override suspend fun loadCollectionDocuments(path: String): List<JsonObject> = emptyList()
    }
}
