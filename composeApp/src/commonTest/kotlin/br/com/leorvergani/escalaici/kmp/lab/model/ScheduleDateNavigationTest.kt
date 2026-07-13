package br.com.leorvergani.escalaici.kmp.lab.model

import br.com.leorvergani.escalaici.kmp.lab.platform.TodayProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScheduleDateNavigationTest {
    private class FakeTodayProvider(private val value: LabDate) : TodayProvider {
        override fun today(): LabDate = value
    }

    private fun summary(vararg days: ShiftDay): ScheduleSummary = ScheduleSummary(
        member = Member("teste@example.invalid", "usuario", "Usuário Teste"),
        team = Team("equipe", "Equipe"),
        days = days.toList(),
        periodLabel = "Período",
        pauseLabel = "--",
        pauseOffsetLabel = "--"
    )

    private fun day(date: LabDate, type: ShiftType) = ShiftDay(
        dayLabel = date.dayOfWeekShort(),
        dateLabel = date.dateLabel(),
        fullDateLabel = date.fullDateLabel(),
        type = type,
        date = date
    )

    @Test
    fun initialDate_usesTodayInsidePeriod() {
        val summary = summary(day(LabDate(2025, 6, 26), ShiftType.MANHA), day(LabDate(2025, 7, 25), ShiftType.FOLGA))
        val today = FakeTodayProvider(LabDate(2025, 7, 10)).today()

        assertEquals(today, initialScheduleDate(summary, today))
        assertTrue(summary.contains(today))
    }

    @Test
    fun initialDate_usesPeriodStartWhenTodayIsOutside() {
        val start = LabDate(2025, 6, 26)
        val summary = summary(day(start, ShiftType.FOLGA), day(LabDate(2025, 7, 25), ShiftType.MANHA))

        assertEquals(start, initialScheduleDate(summary, LabDate(2025, 5, 1)))
        assertEquals(start, initialScheduleDate(summary, LabDate(2025, 8, 1)))
        assertFalse(summary.contains(LabDate(2025, 8, 1)))
    }

    @Test
    fun nextEvents_neverFallBackToOldAssignments() {
        val summary = summary(
            day(LabDate(2024, 12, 26), ShiftType.MANHA),
            day(LabDate(2025, 1, 1), ShiftType.FOLGA)
        )

        assertNull(summary.nextShift(LabDate(2026, 1, 1)))
        assertNull(summary.nextRest(LabDate(2026, 1, 1)))
    }

    @Test
    fun nextEvents_findFutureShiftAndRest() {
        val futureShift = day(LabDate(2027, 1, 2), ShiftType.NOITE)
        val futureRest = day(LabDate(2027, 1, 3), ShiftType.FOLGA)
        val summary = summary(futureRest, futureShift)

        assertEquals(futureShift, summary.nextShift(LabDate(2027, 1, 1)))
        assertEquals(futureRest, summary.nextRest(LabDate(2027, 1, 1)))
    }
}
