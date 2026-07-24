package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.ChangeRequestStatus
import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.ShiftOccurrence
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.model.TemporalState
import br.com.leorvergani.escalaici.model.statusTyped
import br.com.leorvergani.escalaici.source.DemoPublicationGateway
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import br.com.leorvergani.escalaici.ui.changeRequestsRelevantTo
import br.com.leorvergani.escalaici.ui.colleaguesForShift
import br.com.leorvergani.escalaici.ui.effectivePause
import br.com.leorvergani.escalaici.ui.pendingChangeRequestsCountLabel
import br.com.leorvergani.escalaici.ui.pendingChangeRequestsRelevantTo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject

/**
 * Golden contract (FASE 14J, spec 67 seção 7) - fixture sintética, sem dados
 * reais, provando a cadeia completa: pacote (mesmo formato de wire do
 * Dashboard, `DemoFixturePackage`) → DTO (`DemoPublicationSnapshot`/
 * `toScheduleChangeRequests`) → domínio KMP (`DemoPublicationData`) →
 * seletores dos cards (`colleaguesForShift`, `effectivePause`,
 * `changeRequestsRelevantTo`/`pendingChangeRequestsRelevantTo`).
 *
 * Dois usuários (`member-a`/`member-b`), quatro turnos (Md/M/T/N), uma folga,
 * uma pausa personalizada, três solicitações de troca (uma enviada por A, uma
 * enviada por B - que NUNCA pode aparecer para A -, uma aguardando A como
 * responsável designado) e dois workspaces (oficial/Demo) com o mesmo id de
 * membro mas dados distintos, provando que um nunca vaza para o outro.
 */
class GoldenContractTest {
    @Test
    fun officialWorkspaceResolvesOnlyRequesterOwnScheduleAndColleaguesByShift() = runTest {
        val data = officialRepository().data()
        val summary = officialRepository().scheduleSummaryForMember("member-a")

        assertNotNull(summary)
        assertEquals("member-a", summary.member.id)
        assertEquals("team-soc", summary.team.teamId)

        val morningDay = summary.days.single { it.date == LabDate(2026, 7, 27) }
        assertEquals(ShiftType.MANHA, morningDay.type)
        // member-b trabalha Tarde no mesmo dia - colleaguesForShift nunca deve incluir member-b
        // no turno da manha de member-a, so member-c (mesmo turno).
        assertEquals(listOf("Pessoa C"), colleaguesForShift(morningDay))
        assertFalse(colleaguesForShift(morningDay).contains("Pessoa B"))

        val folgaDay = summary.days.single { it.date == LabDate(2026, 7, 28) }
        assertEquals(ShiftType.FOLGA, folgaDay.type)

        // Nenhum outro membro (member-b) aparece na escala resolvida de member-a.
        assertTrue(data.members.any { it.id == "member-b" }) // existe no workspace...
        assertTrue(summary.days.none { it.teamMembers.contains("Pessoa B") && it.type != ShiftType.MANHA })
    }

    @Test
    fun effectivePauseIsConsistentForResolvedShift() = runTest {
        val summary = officialRepository().scheduleSummaryForMember("member-a")
        assertNotNull(summary)

        val morningDay = summary.days.single { it.date == LabDate(2026, 7, 27) }
        val date = requireNotNull(morningDay.date)
        val morningShift = ShiftOccurrence(
            day = morningDay,
            start = LabDateTime(date, 7 * 60),
            end = LabDateTime(date, 13 * 60),
            state = TemporalState.CURRENT
        )
        val pause = effectivePause(
            shift = morningShift,
            settings = NotificationSettings(notifyPause = true, pauseCustomTime = "10:30")
        )

        assertEquals("10:30–10:45", pause?.scheduledLabel)
        assertNotNull(pause?.windowStart)
        assertNotNull(pause?.windowEnd)
    }

    @Test
    fun changeRequestsRelevantToNeverLeaksOtherMemberOwnRequest() = runTest {
        val requests = officialRepository().data().scheduleChangeRequests

        val relevantToA = changeRequestsRelevantTo("member-a", requests).map { it.id }.toSet()
        assertEquals(setOf("cr-sent-by-a", "cr-awaiting-a-approval"), relevantToA)
        assertFalse(relevantToA.contains("cr-sent-by-b"))

        val relevantToB = changeRequestsRelevantTo("member-b", requests).map { it.id }.toSet()
        assertEquals(setOf("cr-sent-by-b", "cr-awaiting-a-approval"), relevantToB)
    }

    @Test
    fun cardBadgeAndFullScreenDeriveFromTheSameSelectorAndData() = runTest {
        val requests = officialRepository().data().scheduleChangeRequests

        // O card (Hoje) e a tela completa (Trocas) usam a mesma lista bruta e o
        // mesmo seletor - nunca dois cálculos divergentes.
        val pendingForCard = pendingChangeRequestsRelevantTo("member-a", requests)
        val relevantForFullScreen = changeRequestsRelevantTo("member-a", requests)
            .filter { it.statusTyped == ChangeRequestStatus.PENDING }

        assertEquals(pendingForCard.map { it.id }.toSet(), relevantForFullScreen.map { it.id }.toSet())
        assertEquals("2 solicitações pendentes", pendingChangeRequestsCountLabel(pendingForCard.size))
    }

    @Test
    fun demoWorkspaceNeverLeaksIntoOfficialWorkspaceOrViceVersa() = runTest {
        val officialRequests = officialRepository().data().scheduleChangeRequests.map { it.id }
        val demoRequests = demoRepository().data().scheduleChangeRequests.map { it.id }

        assertTrue("cr-demo-only" in demoRequests)
        assertFalse("cr-demo-only" in officialRequests)
        assertFalse(officialRequests.any { it in demoRequests })
    }

    private fun officialRepository() = buildRepository(
        workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
        teamId = "team-soc",
        members = listOf(
            fixtureMember("member-a", "Pessoa A"),
            fixtureMember("member-b", "Pessoa B"),
            fixtureMember("member-c", "Pessoa C")
        ),
        assignments = listOf(
            fixtureAssignment("assignment-a-27", "member-a", "2026-07-27", "Manhã"),
            fixtureAssignment("assignment-b-27", "member-b", "2026-07-27", "Tarde"),
            fixtureAssignment("assignment-c-27", "member-c", "2026-07-27", "Manhã"),
            fixtureAssignment("assignment-a-28", "member-a", "2026-07-28", null, assignmentType = "OFF"),
            fixtureAssignment("assignment-b-29", "member-b", "2026-07-29", "Noite"),
            fixtureAssignment("assignment-c-30", "member-c", "2026-07-30", "Madrugada")
        ),
        changeRequests = listOf(
            fixtureChangeRequest(
                id = "cr-sent-by-a",
                requesterMemberId = "member-a",
                assignedManagerMemberId = "manager-x",
                requestType = "SHIFT_CHANGE"
            ),
            fixtureChangeRequest(
                id = "cr-sent-by-b",
                requesterMemberId = "member-b",
                assignedManagerMemberId = "manager-y",
                requestType = "DAY_OFF_CHANGE"
            ),
            fixtureChangeRequest(
                id = "cr-awaiting-a-approval",
                requesterMemberId = "member-b",
                assignedManagerMemberId = "member-a",
                requestType = "SCHEDULE_CORRECTION"
            )
        )
    )

    private fun demoRepository() = buildRepository(
        workspaceId = OrganizationWorkspace.DEMO_WORKSPACE_ID,
        teamId = "team-demo",
        members = listOf(fixtureMember("member-a", "Pessoa A Demo")),
        assignments = listOf(fixtureAssignment("assignment-demo-a-27", "member-a", "2026-07-27", "Manhã", teamId = "team-demo")),
        changeRequests = listOf(
            fixtureChangeRequest(
                id = "cr-demo-only",
                requesterMemberId = "member-a",
                assignedManagerMemberId = "manager-demo",
                requestType = "OTHER",
                teamId = "team-demo"
            )
        )
    )

    private fun buildRepository(
        workspaceId: String,
        teamId: String,
        members: List<DemoFixtureMember>,
        assignments: List<DemoFixtureScheduleAssignment>,
        changeRequests: List<DemoFixtureScheduleChangeRequest>
    ) = DemoPublicationRepository(
        resolver = DemoPublicationResolver(UnavailableGoldenGateway()),
        fixtureProvider = {
            DemoFixturePackage(
                schemaVersion = 1,
                workspace = DemoFixtureWorkspace(
                    workspaceId = workspaceId,
                    workspaceType = if (workspaceId == OrganizationWorkspace.DEMO_WORKSPACE_ID) "DEMO" else "OFFICIAL",
                    scenarioId = "golden-contract",
                    seedVersion = 1,
                    publicationRevision = 2,
                    externalEffectsAllowed = false,
                    notificationsEnabled = false
                ),
                teams = listOf(
                    DemoFixtureTeam(
                        id = teamId,
                        workspaceId = workspaceId,
                        name = "Equipe Golden Contract",
                        acronym = "GC",
                        active = true,
                        schemaVersion = 1
                    )
                ),
                members = members,
                memberTeamMemberships = members.map { member ->
                    DemoFixtureMembership(
                        id = "membership-${member.id}-$workspaceId",
                        workspaceId = workspaceId,
                        memberId = member.id,
                        teamId = teamId,
                        startDate = "2020-01-01",
                        endDate = null,
                        active = true,
                        isPrimary = true,
                        schemaVersion = 1
                    )
                },
                teamManagerAssignments = emptyList(),
                scheduleChangeRequests = changeRequests,
                schedulePeriods = listOf(
                    DemoFixtureSchedulePeriod(
                        id = "period-1",
                        workspaceId = workspaceId,
                        teamId = teamId,
                        name = "Julho 2026",
                        startDate = "2026-07-26",
                        endDate = "2026-08-25",
                        active = true,
                        publicationRevision = 2,
                        schemaVersion = 1
                    )
                ),
                scheduleAssignments = assignments,
                publicationRecords = emptyList()
            )
        }
    )

    private fun fixtureMember(id: String, displayName: String) = DemoFixtureMember(
        id = id,
        workspaceId = "n/a",
        displayName = displayName,
        corporateLogin = "$id.login",
        emailNormalized = "$id@example.invalid",
        active = true,
        schemaVersion = 1
    )

    private fun fixtureAssignment(
        id: String,
        memberId: String,
        date: String,
        shiftName: String?,
        assignmentType: String = "WORK_SHIFT",
        teamId: String = "team-soc"
    ) = DemoFixtureScheduleAssignment(
        id = id,
        workspaceId = "n/a",
        periodId = "period-1",
        teamId = teamId,
        memberId = memberId,
        date = date,
        assignmentType = assignmentType,
        shiftName = shiftName,
        startTime = "08:00",
        endTime = "18:00",
        schemaVersion = 1
    )

    private fun fixtureChangeRequest(
        id: String,
        requesterMemberId: String,
        assignedManagerMemberId: String,
        requestType: String,
        teamId: String = "team-soc"
    ) = DemoFixtureScheduleChangeRequest(
        id = id,
        workspaceId = "n/a",
        requesterMemberId = requesterMemberId,
        requesterTeamId = teamId,
        assignedManagerMemberId = assignedManagerMemberId,
        schedulePeriodId = "period-1",
        assignmentId = null,
        requestType = requestType,
        status = "PENDING",
        reason = "Motivo sintético de teste (golden contract)",
        createdAt = "2026-07-20T00:00:00Z",
        resolvedAt = null,
        resolvedByMemberId = null,
        resolutionNote = null,
        schemaVersion = 1
    )

    private class UnavailableGoldenGateway : DemoPublicationGateway {
        override suspend fun loadDocumentFields(path: String): JsonObject {
            error("404 Not Found")
        }

        override suspend fun loadCollectionDocuments(path: String): List<JsonObject> = emptyList()
    }
}
