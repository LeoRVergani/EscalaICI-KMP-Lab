package br.com.leorvergani.escalaici.model

/**
 * FASE 9d — regras puras de resumo da semana e alertas, descritas na spec
 * `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (secao 4, repositorio Android
 * principal). Operam sobre `ScheduleAssignment` (modelo puro da FASE 9c),
 * sem depender da UI mock (`ShiftDay`/`ScheduleSummary`) nem de Android/JVM.
 */

data class WeekSummary(
    val workedDays: Int,
    val restDays: Int,
    val totalHours: Int,
    val nextShift: ScheduleAssignment?,
    val nextRest: ScheduleAssignment?
)

fun weekSummaryOf(assignments: List<ScheduleAssignment>): WeekSummary {
    val sorted = assignments.sortedBy { it.date }
    val workedDays = sorted.count { it.shiftType.isWorkShift }
    return WeekSummary(
        workedDays = workedDays,
        restDays = sorted.size - workedDays,
        totalHours = workedDays * 6,
        nextShift = sorted.firstOrNull { it.shiftType.isWorkShift },
        nextRest = sorted.firstOrNull { !it.shiftType.isWorkShift }
    )
}

enum class ScheduleAlertSeverity {
    INFO,
    ATENCAO,
    CRITICO
}

data class ScheduleAlert(
    val title: String,
    val message: String,
    val severity: ScheduleAlertSeverity,
    val date: String? = null
)

object ScheduleAlertRules {
    private const val MinimumRestMinutes = 11 * 60
    private const val MaxConsecutiveWorkDays = 6
    private const val MinutesPerDay = 24 * 60

    operator fun invoke(assignments: List<ScheduleAssignment>): List<ScheduleAlert> {
        if (assignments.isEmpty()) return emptyList()
        val sorted = assignments.sortedBy { it.date }

        val alerts = mutableListOf<ScheduleAlert>()
        alerts += restAlerts(sorted)
        alerts += sixByOneAlerts(sorted)
        alerts += inconsistencyAlerts(sorted)
        return alerts
    }

    private fun restAlerts(sorted: List<ScheduleAssignment>): List<ScheduleAlert> {
        val workShifts = sorted.filter {
            it.shiftType.isWorkShift && it.shiftType.startMinute != null && it.shiftType.endMinute != null
        }

        return workShifts.zipWithNext().mapNotNull { (previous, next) ->
            val previousEnd = previous.absoluteEndMinute() ?: return@mapNotNull null
            val nextStart = next.absoluteStartMinute() ?: return@mapNotNull null
            val restMinutes = nextStart - previousEnd
            if (restMinutes < MinimumRestMinutes) {
                ScheduleAlert(
                    title = "Descanso menor que 11h",
                    message = "${previous.date} ${previous.shiftType.label} -> ${next.date} ${next.shiftType.label}: " +
                        "${restMinutes / 60}h de descanso. Mínimo recomendado: 11h.",
                    severity = ScheduleAlertSeverity.CRITICO,
                    date = next.date
                )
            } else {
                null
            }
        }
    }

    private fun sixByOneAlerts(sorted: List<ScheduleAssignment>): List<ScheduleAlert> {
        val alerts = mutableListOf<ScheduleAlert>()
        var sequence = 0
        var start: String? = null
        var previousDate: LabDate? = null

        sorted.forEach { assignment ->
            val date = LabDate.parseIso(assignment.date)
            if (previousDate == null || date == null || previousDate.plusDays(1) != date) {
                sequence = 0
                start = null
            }
            if (assignment.shiftType.isWorkShift) {
                if (sequence == 0) start = assignment.date
                sequence += 1
                if (sequence > MaxConsecutiveWorkDays) {
                    alerts += ScheduleAlert(
                        title = "Regra 6x1 excedida",
                        message = "Sequência de $sequence dias trabalhados desde ${start ?: "data desconhecida"}.",
                        severity = ScheduleAlertSeverity.CRITICO,
                        date = assignment.date
                    )
                }
            } else {
                sequence = 0
                start = null
            }
            previousDate = date
        }
        return alerts
    }

    private fun inconsistencyAlerts(sorted: List<ScheduleAssignment>): List<ScheduleAlert> {
        return sorted.filter { it.shiftType == ShiftType.INCONSISTENCIA || it.shiftType == ShiftType.INDEFINIDO }
            .map { assignment ->
                ScheduleAlert(
                    title = if (assignment.shiftType == ShiftType.INCONSISTENCIA) {
                        "Inconsistência na escala"
                    } else {
                        "Turno indefinido"
                    },
                    message = "${assignment.date}: ${assignment.shiftType.label}. ${assignment.notes.orEmpty()}".trim(),
                    severity = if (assignment.shiftType == ShiftType.INCONSISTENCIA) {
                        ScheduleAlertSeverity.ATENCAO
                    } else {
                        ScheduleAlertSeverity.INFO
                    },
                    date = assignment.date
                )
            }
    }

    private fun ScheduleAssignment.absoluteStartMinute(): Int? {
        val date = LabDate.parseIso(date) ?: return null
        val startMinute = shiftType.startMinute ?: return null
        return date.epochDay() * MinutesPerDay + startMinute
    }

    private fun ScheduleAssignment.absoluteEndMinute(): Int? {
        val date = LabDate.parseIso(date) ?: return null
        val endMinute = shiftType.endMinute ?: return null
        val endDay = date.epochDay() + if (shiftType.crossesMidnight) 1 else 0
        return endDay * MinutesPerDay + endMinute
    }
}
