package br.com.leorvergani.escalaici.kmp.lab.model

data class LabAlert(
    val title: String,
    val message: String,
    val severity: Severity,
    val date: LabDate? = null
) {
    enum class Severity(val label: String) {
        INFO("Info"),
        ATENCAO("Atenção"),
        CRITICO("Crítico")
    }
}

object GenerateLabAlerts {
    operator fun invoke(summary: ScheduleSummary): List<LabAlert> {
        val days = summary.days.sortedBy { it.date }
        if (days.isEmpty()) return emptyList()

        val alerts = mutableListOf<LabAlert>()
        alerts += LabAlert(
            title = "Fonte da escala",
            message = "${summary.sourceFileName ?: "Demonstração local"} • ${summary.periodLabel} • ${summary.member.scaleName}",
            severity = LabAlert.Severity.INFO,
            date = days.firstOrNull()?.date
        )

        generateRestAlerts(days, alerts)
        generateSixByOneAlerts(days, alerts)
        generateInconsistencyAlerts(days, alerts)
        summary.errors.forEach { error ->
            alerts += LabAlert(
                title = "Erro de importação",
                message = error,
                severity = LabAlert.Severity.CRITICO
            )
        }
        summary.warnings.forEach { warning ->
            val critical = warning.contains("6x1", ignoreCase = true) ||
                warning.contains("divergência", ignoreCase = true) ||
                warning.contains("divergencia", ignoreCase = true)
            alerts += LabAlert(
                title = if (critical) "Alerta da escala" else "Aviso da escala",
                message = warning,
                severity = if (critical) LabAlert.Severity.ATENCAO else LabAlert.Severity.INFO
            )
        }

        if (alerts.none { it.title.contains("6x1") && it.severity == LabAlert.Severity.CRITICO }) {
            alerts += LabAlert(
                title = "Regra 6x1 dentro do limite",
                message = "Nenhuma sequência acima de 6 dias trabalhados foi encontrada.",
                severity = LabAlert.Severity.INFO,
                date = days.firstOrNull()?.date
            )
        }

        return alerts.distinctBy { "${it.title}|${it.message}|${it.severity}|${it.date}" }
    }

    private fun generateRestAlerts(days: List<ShiftDay>, alerts: MutableList<LabAlert>) {
        val workDays = days
            .filter { it.date != null && it.type.isWorkShift && it.type.startMinute != null && it.type.endMinute != null }
            .sortedBy { it.date }

        workDays.zipWithNext().forEach { (previous, next) ->
            val rest = next.startAbsoluteMinute() - previous.endAbsoluteMinute()
            if (rest < 11 * 60) {
                alerts += LabAlert(
                    title = "Descanso menor que 11h",
                    message = "${previous.dateLabel} ${previous.type.label} -> ${next.dateLabel} ${next.type.label}: ${rest / 60}h de descanso. Mínimo recomendado: 11h.",
                    severity = LabAlert.Severity.CRITICO,
                    date = next.date
                )
            }
        }
    }

    private fun generateSixByOneAlerts(days: List<ShiftDay>, alerts: MutableList<LabAlert>) {
        var sequence = 0
        var start: LabDate? = null

        days.sortedBy { it.date }.forEach { day ->
            if (day.type.isWorkShift) {
                if (sequence == 0) start = day.date
                sequence += 1
                if (sequence > 6) {
                    alerts += LabAlert(
                        title = "Regra 6x1 excedida",
                        message = "Sequência de $sequence dias trabalhados desde ${start?.dateLabel() ?: "--/--"}.",
                        severity = LabAlert.Severity.CRITICO,
                        date = day.date
                    )
                }
            } else {
                sequence = 0
                start = null
            }
        }
    }

    private fun generateInconsistencyAlerts(days: List<ShiftDay>, alerts: MutableList<LabAlert>) {
        days.filter { it.type == ShiftType.INCONSISTENCIA || it.type == ShiftType.INDEFINIDO }.forEach { day ->
            alerts += LabAlert(
                title = if (day.type == ShiftType.INCONSISTENCIA) "Inconsistência na escala" else "Turno indefinido",
                message = "${day.dateLabel}: ${day.type.label}. ${day.note.orEmpty()}".trim(),
                severity = if (day.type == ShiftType.INCONSISTENCIA) LabAlert.Severity.ATENCAO else LabAlert.Severity.INFO,
                date = day.date
            )
        }
    }

    private fun ShiftDay.startAbsoluteMinute(): Int {
        val date = requireNotNull(date)
        return date.epochDay() * MinutesPerDay + requireNotNull(type.startMinute)
    }

    private fun ShiftDay.endAbsoluteMinute(): Int {
        val date = requireNotNull(date)
        val endDay = date.epochDay() + if (type.crossesMidnight) 1 else 0
        return endDay * MinutesPerDay + requireNotNull(type.endMinute)
    }

    private const val MinutesPerDay = 24 * 60
}
