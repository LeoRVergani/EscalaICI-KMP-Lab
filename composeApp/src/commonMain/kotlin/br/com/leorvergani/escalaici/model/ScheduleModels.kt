package br.com.leorvergani.escalaici.model

enum class ShiftType(
    val label: String,
    val shortLabel: String,
    val timeRange: String,
    val isWorkShift: Boolean,
    val startMinute: Int? = null,
    val endMinute: Int? = null,
    val crossesMidnight: Boolean = false
) {
    MADRUGADA("Madrugada", "Md", "01:00 - 07:00", true, startMinute = 60, endMinute = 420),
    MANHA("Manhã", "M", "07:00 - 13:00", true, startMinute = 420, endMinute = 780),
    TARDE("Tarde", "T", "13:00 - 19:00", true, startMinute = 780, endMinute = 1140),
    NOITE("Noite", "N", "19:00 - 01:00", true, startMinute = 1140, endMinute = 60, crossesMidnight = true),
    FOLGA("Folga", "F", "Descanso", false),
    FERIAS("Férias", "X", "Férias", false),
    BH("BH", "BH", "Banco de horas", false),
    ANIVERSARIO("Aniversário", "An", "Folga aniversário", false),
    HORA_EXTRA("Hora extra", "HE", "Hora extra", true),
    AFASTAMENTO("Afastamento", "Af", "Afastamento", false),
    INCONSISTENCIA("Inconsistência", "!", "Verificar escala", false),
    INDEFINIDO("Indefinido", "?", "Sem turno definido", false)
}

data class ShiftDay(
    val dayLabel: String,
    val dateLabel: String,
    val fullDateLabel: String,
    val type: ShiftType,
    val date: LabDate? = null,
    val teamMembers: List<String> = emptyList(),
    val membersByShift: Map<ShiftType, List<String>> = emptyMap(),
    val sourceStatus: String? = null,
    val note: String? = null,
    /**
     * Rótulo apresentável, distinto de `type.label` para alguns tipos (ex.:
     * BH -> "Banco de horas", ANIVERSARIO -> "Folga aniversário", FOLGA com
     * `sourceStatus` -> "Folga / <status>"), igual ao `labelFor()` do parser
     * oficial (`ScaleWorkbookParser.kt`). Default cai para `type.label` para
     * não quebrar os mocks existentes, que não passam por esse cálculo.
     */
    val label: String = type.label
)

data class ScheduleSummary(
    val member: Member,
    val team: Team,
    val days: List<ShiftDay>,
    val periodLabel: String,
    val pauseLabel: String,
    val pauseOffsetLabel: String,
    /**
     * Janela permitida para a pausa (ex.: início "09:00", fim "11:45"),
     * igual ao `getPauseWindow()` do app real (`PauseWindow.kt`: início do
     * turno + 120min até início do turno + 285min). Distinta de
     * `pauseLabel` (horário sugerido específico) e de `pauseOffsetLabel`
     * (texto de offset, ex. "1h após o início") — o app real mostra as
     * três coisas em textos diferentes.
     */
    val pauseWindowStart: String = "--:--",
    val pauseWindowEnd: String = "--:--",
    /** Horários sugeridos dentro da janela (6 opções, a cada 30min), igual
     *  ao `suggestedPauseTimes()` do app real. */
    val pauseSuggestions: List<String> = emptyList(),
    val sourceFileName: String? = null,
    val sheetNames: List<String> = emptyList(),
    val collaborators: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
    val periodStart: LabDate? = days.mapNotNull { it.date }.minOrNull(),
    val periodEnd: LabDate? = days.mapNotNull { it.date }.maxOrNull()
) {
    /**
     * "Hoje" real (data do dispositivo/navegador via `todayLabDate()`),
     * não o primeiro dia da lista — assim `nextShift`/`nextRest` sempre
     * refletem a data real atual, igual em todas as abas (Hoje, Escala,
     * Alertas), em vez de ficar preso ao primeiro dia do arquivo
     * importado.
     */
    fun nextShift(today: LabDate): ShiftDay? = days
        .filter { it.type.isWorkShift && it.date != null && it.date!! >= today }
        .minByOrNull { it.date!! }

    fun nextRest(today: LabDate): ShiftDay? = days
        .filter { !it.type.isWorkShift && it.date != null && it.date!! >= today }
        .minByOrNull { it.date!! }

    /**
     * Ciclo fixo do período (dia 26 de um mês até dia 25 do próximo),
     * ancorado em "hoje" real — não no primeiro/último dia do arquivo
     * importado. Protege o resumo contra dados fora do ciclo esperado
     * (ex.: linhas extras na planilha), mesmo que o app real não valide
     * isso explicitamente (ele confia que a leitura fixa de 30 linhas da
     * aba Escala já é sempre um ciclo só).
     */
    private val periodDays: List<ShiftDay>
        get() {
            val start = periodStart ?: return days.filter { it.date != null }
            val end = periodEnd ?: return days.filter { it.date != null }
            return days.filter { day -> day.date != null && day.date!! >= start && day.date!! <= end }
        }

    fun contains(date: LabDate): Boolean = periodStart?.let { start ->
        periodEnd?.let { end -> date in start..end }
    } ?: false

    val workedDays: Int
        get() = periodDays.count { it.type.isWorkShift }

    val restDays: Int
        get() = periodDays.size - workedDays

    val totalHours: Int
        get() = workedDays * 6

    val isImported: Boolean
        get() = sourceFileName != null
}

fun initialScheduleDate(summary: ScheduleSummary, today: LabDate): LabDate? {
    val start = summary.periodStart ?: summary.days.mapNotNull { it.date }.minOrNull() ?: return null
    val end = summary.periodEnd ?: summary.days.mapNotNull { it.date }.maxOrNull() ?: return start
    return if (today in start..end) today else start
}

data class Member(
    val email: String,
    val scaleName: String,
    val displayName: String,
    val id: String = email,
    val teamId: String = "",
    val role: MemberRole = MemberRole.ANALYST,
    val active: Boolean = true,
    val workspaceId: String? = null,
    val publicationRevision: Int? = null,
    val entraTenantId: String? = null,
    val entraObjectId: String? = null
)

data class Team(
    val teamId: String,
    val name: String,
    val id: String = teamId,
    val displayName: String = name,
    val members: List<Member> = emptyList(),
    val workspaceId: String? = null,
    val publicationRevision: Int? = null
)

data class LabDate(
    val year: Int,
    val month: Int,
    val day: Int
) : Comparable<LabDate> {
    override fun compareTo(other: LabDate): Int {
        return compareValuesBy(this, other, LabDate::year, LabDate::month, LabDate::day)
    }

    fun dateLabel(): String = "${day.twoDigits()}/${month.twoDigits()}"

    fun fullDateLabel(): String = "${dayOfWeekLong()}, ${dateLabel()}/$year"

    fun periodToken(): String = "$day ${monthShortPt()}"

    fun dayOfWeekShort(): String = dayOfWeekNamesShort[dayOfWeekIndex()]

    fun dayOfWeekSundayIndex(): Int = (dayOfWeekIndex() + 1) % 7

    fun yearMonth(): LabYearMonth = LabYearMonth(year, month)

    fun epochDay(): Int {
        val yearsBefore = year - 1
        val leapDays = yearsBefore / 4 - yearsBefore / 100 + yearsBefore / 400
        val daysBeforeYear = yearsBefore * 365 + leapDays
        val daysBeforeMonth = (1 until month).sumOf { monthLength(year, it) }
        return daysBeforeYear + daysBeforeMonth + day
    }

    private fun dayOfWeekLong(): String = dayOfWeekNamesLong[dayOfWeekIndex()]

    private fun monthShortPt(): String = MonthShortNames[(month - 1).coerceIn(0, 11)]

    private fun dayOfWeekIndex(): Int {
        var y = year
        var m = month
        if (m < 3) {
            m += 12
            y -= 1
        }
        val k = y % 100
        val j = y / 100
        val h = (day + (13 * (m + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
        return when (h) {
            0 -> 5
            1 -> 6
            2 -> 0
            3 -> 1
            4 -> 2
            5 -> 3
            else -> 4
        }
    }

    private fun Int.twoDigits(): String = toString().padStart(2, '0')

    companion object {
        private val dayOfWeekNamesShort = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")
        private val dayOfWeekNamesLong = listOf(
            "segunda-feira",
            "terça-feira",
            "quarta-feira",
            "quinta-feira",
            "sexta-feira",
            "sábado",
            "domingo"
        )
        val MonthShortNames = listOf("jan.", "fev.", "mar.", "abr.", "mai.", "jun.", "jul.", "ago.", "set.", "out.", "nov.", "dez.")
        val MonthLongNames = listOf("janeiro", "fevereiro", "março", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")

        fun monthLength(year: Int, month: Int): Int = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

        private fun isLeapYear(year: Int): Boolean {
            return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        }

        /** Parseia datas no formato ISO `yyyy-MM-dd` usadas pelos modelos puros da FASE 9c/9d. */
        fun parseIso(value: String): LabDate? {
            val parts = value.split("-")
            if (parts.size != 3) return null
            val year = parts[0].toIntOrNull() ?: return null
            val month = parts[1].toIntOrNull() ?: return null
            val day = parts[2].toIntOrNull() ?: return null
            return LabDate(year, month, day)
        }
    }
}

data class LabYearMonth(
    val year: Int,
    val month: Int
) : Comparable<LabYearMonth> {
    override fun compareTo(other: LabYearMonth): Int {
        return compareValuesBy(this, other, LabYearMonth::year, LabYearMonth::month)
    }

    fun monthTitle(): String {
        val name = LabDate.MonthLongNames[(month - 1).coerceIn(0, 11)]
        return name.replaceFirstChar { it.uppercase() } + " de $year"
    }

    fun periodMonthLabel(): String {
        return "${LabDate.MonthShortNames[(month - 1).coerceIn(0, 11)]} $year"
    }

    fun lengthOfMonth(): Int = LabDate.monthLength(year, month)

    fun atDay(day: Int): LabDate = LabDate(year, month, day.coerceIn(1, lengthOfMonth()))

    fun firstDayOffsetSunday(): Int = atDay(1).dayOfWeekSundayIndex()

    fun plusMonths(offset: Int): LabYearMonth {
        val zeroBased = year * 12 + (month - 1) + offset
        val newYear = zeroBased.floorDiv(12)
        val newMonth = zeroBased.mod(12) + 1
        return LabYearMonth(newYear, newMonth)
    }

    fun isBefore(other: LabYearMonth): Boolean = this < other

    fun isAfter(other: LabYearMonth): Boolean = this > other

    private fun Int.floorDiv(other: Int): Int {
        val result = this / other
        return if ((this xor other) < 0 && result * other != this) result - 1 else result
    }

    private fun Int.mod(other: Int): Int {
        val result = this % other
        return if (result < 0) result + other else result
    }
}
