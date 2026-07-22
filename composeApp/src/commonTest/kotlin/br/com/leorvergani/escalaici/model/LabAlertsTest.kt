package br.com.leorvergani.escalaici.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LabAlertsTest {
    @Test
    fun sixByOneSequenceResetsAcrossMissingCivilDay() {
        val workedDays = listOf(1, 2, 3, 5, 6, 7, 8).map { day ->
            val date = LabDate(2026, 7, day)
            ShiftDay(
                dayLabel = date.dayOfWeekShort(),
                dateLabel = date.dateLabel(),
                fullDateLabel = date.fullDateLabel(),
                type = ShiftType.MANHA,
                date = date
            )
        }

        val alerts = GenerateLabAlerts(summaryWithDays(workedDays))

        assertTrue(alerts.none { it.title == "Regra 6x1 excedida" })
    }

    @Test
    fun undefinedShiftsUpToThresholdGenerateOneAlertPerDay() {
        val alerts = GenerateLabAlerts(summaryWithUndefinedDays(3))

        assertEquals(3, alerts.count { it.title == "Turno indefinido" })
        assertTrue(alerts.none { it.title == "Turno indefinido em vários dias" })
    }

    @Test
    fun undefinedShiftsAboveThresholdGenerateSingleConsolidatedAlert() {
        val alerts = GenerateLabAlerts(summaryWithUndefinedDays(4))

        assertEquals(0, alerts.count { it.title == "Turno indefinido" })
        assertEquals(1, alerts.count { it.title == "Turno indefinido em vários dias" })
        assertTrue(alerts.any { it.message.contains("4 dias sem turno reconhecido") })
    }

    private fun summaryWithUndefinedDays(count: Int): ScheduleSummary {
        val days = (1..count).map { day ->
            val date = LabDate(2026, 7, day)
            ShiftDay(
                dayLabel = date.dayOfWeekShort(),
                dateLabel = date.dateLabel(),
                fullDateLabel = date.fullDateLabel(),
                type = ShiftType.INDEFINIDO,
                date = date
            )
        }
        return summaryWithDays(days)
    }

    private fun summaryWithDays(days: List<ShiftDay>): ScheduleSummary {
        return ScheduleSummary(
            member = Member(
                email = "pessoa@example.invalid",
                scaleName = "pessoa",
                displayName = "Pessoa"
            ),
            team = Team(teamId = "team", name = "Team"),
            days = days,
            periodLabel = "Julho 2026",
            pauseLabel = "--:--",
            pauseOffsetLabel = ""
        )
    }
}
