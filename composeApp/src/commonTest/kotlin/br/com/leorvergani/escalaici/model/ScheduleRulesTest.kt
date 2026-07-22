package br.com.leorvergani.escalaici.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleRulesTest {

    private fun assignment(
        date: String,
        shiftType: ShiftType,
        notes: String? = null
    ) = ScheduleAssignment(
        id = "assign-$date-${shiftType.name}",
        periodId = "period-test",
        teamId = "soc",
        memberId = "lvergani@ici.tec.br",
        memberName = "lvergani",
        date = date,
        shiftType = shiftType,
        notes = notes
    )

    @Test
    fun weekSummaryOf_countsWorkedAndRestDaysFromMockAssignments() {
        val summary = weekSummaryOf(mockScheduleAssignments())

        assertEquals(1, summary.workedDays)
        assertEquals(1, summary.restDays)
        assertEquals(6, summary.totalHours)
        assertEquals("2026-07-06", summary.nextShift?.date)
        assertEquals("2026-07-11", summary.nextRest?.date)
    }

    @Test
    fun weekSummaryOf_returnsNoNextShiftWhenAllDaysAreRest() {
        val assignments = listOf(
            assignment("2026-07-06", ShiftType.FOLGA),
            assignment("2026-07-07", ShiftType.FERIAS)
        )

        val summary = weekSummaryOf(assignments)

        assertEquals(0, summary.workedDays)
        assertEquals(2, summary.restDays)
        assertEquals(0, summary.totalHours)
        assertEquals(null, summary.nextShift)
        assertEquals("2026-07-06", summary.nextRest?.date)
    }

    @Test
    fun scheduleAlertRules_flagsRestBelowElevenHours() {
        val assignments = listOf(
            assignment("2026-07-06", ShiftType.NOITE),
            assignment("2026-07-07", ShiftType.MANHA)
        )

        val alerts = ScheduleAlertRules(assignments)

        assertTrue(alerts.any { it.title == "Descanso menor que 11h" && it.severity == ScheduleAlertSeverity.CRITICO })
    }

    @Test
    fun scheduleAlertRules_flagsSequenceAboveSixWorkedDays() {
        val assignments = (6..13).map { day ->
            assignment("2026-07-${day.toString().padStart(2, '0')}", ShiftType.MANHA)
        }

        val alerts = ScheduleAlertRules(assignments)

        assertTrue(alerts.any { it.title == "Regra 6x1 excedida" && it.severity == ScheduleAlertSeverity.CRITICO })
    }

    @Test
    fun scheduleAlertRules_resetsSixByOneSequenceAcrossMissingCivilDay() {
        val assignments = listOf(1, 2, 3, 5, 6, 7, 8).map { day ->
            assignment("2026-07-${day.toString().padStart(2, '0')}", ShiftType.MANHA)
        }

        val alerts = ScheduleAlertRules(assignments)

        assertTrue(alerts.none { it.title == "Regra 6x1 excedida" })
    }

    @Test
    fun scheduleAlertRules_flagsInconsistencyAndUndefinedShifts() {
        val assignments = listOf(
            assignment("2026-07-06", ShiftType.INCONSISTENCIA, notes = "Colaborador não encontrado"),
            assignment("2026-07-07", ShiftType.INDEFINIDO)
        )

        val alerts = ScheduleAlertRules(assignments)

        assertTrue(alerts.any { it.title == "Inconsistência na escala" && it.severity == ScheduleAlertSeverity.ATENCAO })
        assertTrue(alerts.any { it.title == "Turno indefinido" && it.severity == ScheduleAlertSeverity.INFO })
    }

    @Test
    fun scheduleAlertRules_returnsEmptyForEmptyInput() {
        assertEquals(emptyList(), ScheduleAlertRules(emptyList()))
    }
}
