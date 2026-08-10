package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Célula da grade da competência operacional (FASE 17C). `date == null` é
 * preenchimento - antes do início ou depois do fim do período, nunca
 * clicável, nunca um dia real da escala.
 */
data class PeriodCalendarCell(val date: LabDate?)

/**
 * Grade única e contínua cobrindo `start..end` - a competência operacional
 * real (ex. 26/07→25/08), nunca um mês civil isolado. `start`/`end` devem
 * vir de `ScheduleSummary.periodStart`/`periodEnd` (por sua vez lidos de
 * `turnosMes.periodoInicio`/`periodoFim` no caminho Firebase) - este helper
 * nunca recalcula o período, só preenche a grade em torno dele para
 * alinhar domingo→sábado. Tamanho do resultado é sempre múltiplo de 7.
 */
fun buildPeriodCalendarGrid(start: LabDate, end: LabDate): List<PeriodCalendarCell> {
    require(start <= end) { "start ($start) deve ser <= end ($end)" }
    val cells = mutableListOf<PeriodCalendarCell>()
    repeat(start.dayOfWeekSundayIndex()) { cells += PeriodCalendarCell(null) }
    var current = start
    while (current <= end) {
        cells += PeriodCalendarCell(current)
        current = current.plusDays(1)
    }
    val remainder = cells.size % 7
    if (remainder != 0) repeat(7 - remainder) { cells += PeriodCalendarCell(null) }
    return cells
}

/**
 * Título da competência - mesmo mês/ano → "Agosto de 2026"; meses
 * diferentes, mesmo ano → "Julho — agosto de 2026"; anos diferentes →
 * "Dezembro de 2026 — janeiro de 2027". Porte conceitual do
 * `competenciaHeaderLabel` do EscalaSOC (`PeriodCalendarGrid.kt`),
 * adaptado aos nomes de mês já existentes em `LabDate.MonthLongNames`.
 */
fun competenciaHeaderLabel(start: LabDate, end: LabDate): String {
    val startMonth = LabDate.MonthLongNames[(start.month - 1).coerceIn(0, 11)]
    val endMonth = LabDate.MonthLongNames[(end.month - 1).coerceIn(0, 11)]
    val startMonthCapitalized = startMonth.replaceFirstChar { it.uppercase() }
    return when {
        start.year == end.year && start.month == end.month -> "$startMonthCapitalized de ${start.year}"
        start.year == end.year -> "$startMonthCapitalized — $endMonth de ${start.year}"
        else -> "$startMonthCapitalized de ${start.year} — $endMonth de ${end.year}"
    }
}
