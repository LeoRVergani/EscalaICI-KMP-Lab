package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemporalRulesTest {
    private val member = Member("teste@example.invalid", "teste", "Pessoa Teste")
    private val team = Team("teste", "Equipe Teste")

    private fun summary(date: LabDate, type: ShiftType) = ScheduleSummary(
        member, team,
        listOf(ShiftDay("", "", "", type, date)),
        "", "", ""
    )

    @Test fun currentMorningShiftUsesDocumentedWindow() {
        val result = summary(LabDate(2026, 7, 13), ShiftType.MANHA)
            .relevantShift(LabDateTime(LabDate(2026, 7, 13), 8 * 60))
        assertEquals(TemporalState.CURRENT, result?.state)
        assertEquals("09:00–11:45", pauseFor(result)?.displayValue)
        assertNull(pauseFor(result)?.scheduledLabel)
    }

    @Test fun upcomingAndFinishedAreDistinguished() {
        val schedule = summary(LabDate(2026, 7, 13), ShiftType.MANHA)
        assertEquals(TemporalState.UPCOMING, schedule.relevantShift(LabDateTime(LabDate(2026, 7, 13), 6 * 60))?.state)
        assertNull(schedule.relevantShift(LabDateTime(LabDate(2026, 7, 13), 14 * 60)))
    }

    @Test fun nightShiftRemainsCurrentAfterMidnight() {
        val result = summary(LabDate(2026, 7, 13), ShiftType.NOITE)
            .relevantShift(LabDateTime(LabDate(2026, 7, 14), 30))
        assertEquals(TemporalState.CURRENT, result?.state)
        assertEquals("21:00–23:45", pauseFor(result)?.displayValue)
    }

    @Test fun onCallSelectionNeverReturnsFinishedAssignment() {
        val assignment = OnCallAssignment("1", "p", "t", "m", "Pessoa Teste", "2026-07-13", "2026-07-13", "2026-07-14", "19:00", "07:00", OnCallStatus.SCHEDULED)
        assertEquals(TemporalState.CURRENT, relevantOnCall(listOf(assignment), LabDateTime(LabDate(2026, 7, 14), 30))?.state)
        assertNull(relevantOnCall(listOf(assignment), LabDateTime(LabDate(2026, 7, 14), 8 * 60)))
        assertEquals(setOf(LabDate(2026, 7, 13), LabDate(2026, 7, 14)), onCallDates(assignment))
    }
}
