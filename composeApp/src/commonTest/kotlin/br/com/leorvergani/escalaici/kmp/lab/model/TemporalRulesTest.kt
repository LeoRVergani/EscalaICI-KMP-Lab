package br.com.leorvergani.escalaici.kmp.lab.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemporalRulesTest {
    private val date = LabDate(2026, 7, 13)

    @Test fun currentShiftUsesDateAndTime() {
        assertEquals(TemporalState.CURRENT, summary(ShiftType.MANHA).relevantShift(LabDateTime(date, 8 * 60))?.state)
    }

    @Test fun upcomingShiftBeforeStart() {
        assertEquals(TemporalState.UPCOMING, summary(ShiftType.MANHA).relevantShift(LabDateTime(date, 6 * 60))?.state)
    }

    @Test fun finishedShiftIsNotReturnedAsUpcoming() {
        assertNull(summary(ShiftType.MANHA).relevantShift(LabDateTime(date, 14 * 60)))
    }

    @Test fun shiftCrossingMidnightRemainsCurrent() {
        val summary = summary(ShiftType.NOITE)
        assertEquals(TemporalState.CURRENT, summary.relevantShift(LabDateTime(date.plusDays(1), 30))?.state)
    }

    @Test fun pauseIsCalculatedInsideShift() {
        val pause = pauseFor(summary(ShiftType.MANHA).relevantShift(LabDateTime(date, 8 * 60)))
        assertEquals("08:00–08:15", pause?.label)
        assertEquals("1h após o início", pause?.offsetLabel)
    }

    @Test fun missingShiftConfigurationDoesNotInventPause() {
        assertNull(pauseFor(summary(ShiftType.HORA_EXTRA).relevantShift(LabDateTime(date, 0))))
    }

    @Test fun onCallCurrentUpcomingAndFinished() {
        val assignment = onCall("2026-07-13", "19:00", "2026-07-14", "01:00")
        assertEquals(TemporalState.UPCOMING, relevantOnCall(listOf(assignment), LabDateTime(date, 18 * 60))?.state)
        assertEquals(TemporalState.CURRENT, relevantOnCall(listOf(assignment), LabDateTime(date.plusDays(1), 30))?.state)
        assertNull(relevantOnCall(listOf(assignment), LabDateTime(date.plusDays(1), 2 * 60)))
    }

    @Test fun onCallMarkersContainOnlyCoveredDates() {
        val dates = onCallDates(onCall("2026-07-31", "19:00", "2026-08-01", "07:00"))
        assertEquals(setOf(LabDate(2026, 7, 31), LabDate(2026, 8, 1)), dates)
        assertTrue(LabDate(2026, 7, 30) !in dates)
    }

    private fun summary(type: ShiftType): ScheduleSummary = mockScheduleSummary().copy(
        days = listOf(ShiftDay("Seg", "13/07", "segunda-feira, 13/07/2026", type, date = date)),
        periodStart = date,
        periodEnd = date
    )

    private fun onCall(startDate: String, startTime: String, endDate: String, endTime: String) = OnCallAssignment(
        id = "sanitized", periodId = "period", teamId = "team", memberId = "member",
        memberName = "Pessoa Teste", date = startDate, startDate = startDate, endDate = endDate,
        startTime = startTime, endTime = endTime, status = OnCallStatus.SCHEDULED
    )
}
