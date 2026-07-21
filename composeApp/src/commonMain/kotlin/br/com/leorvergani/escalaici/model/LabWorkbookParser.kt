package br.com.leorvergani.escalaici.model

/**
 * Parser proprio do laboratorio (codigo novo, nao copiado) cujas REGRAS
 * foram alinhadas na FASE 11.2 com o parser oficial
 * `ScaleWorkbookParser.kt` do app Android real (`EscalaSOC`, so leitura),
 * lendo o mesmo layout fixo de planilha: aba "Escalistas" com nomes a
 * partir da linha 2 (0-index) e status diarios nas colunas 3..32 da linha
 * 2; aba "Escala" com 30 linhas fixas (2..31), data na coluna 0, turnos nas
 * colunas 2..5 (Madrugada/Manha/Tarde/Noite) e observacoes na coluna 6.
 *
 * Limitacao conhecida (nao corrigida nesta fase): datas em celulas com
 * formatacao Excel que nao renderizam como `dd/MM/yyyy`/`d/M/yyyy`/`dd/MM`/
 * `d/M` via `DataFormatter`/SheetJS podem falhar a leitura aqui, onde o
 * parser oficial tem um atalho lendo o valor de data cru da celula POI.
 * Nao afeta o arquivo real usado hoje (`Escala-SOC-Controle-Atual.xls`,
 * confirmado na FASE 11.1/11.1b) — documentado para retomada futura.
 */
object LabWorkbookParser {
    fun parse(
        workbook: ImportedWorkbook,
        requestedCollaborator: String? = DefaultCollaborator,
        confirmedStartYear: Int? = null
    ): ScheduleImportPreview {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val escalaSheet = workbook.sheets.firstOrNull { it.name.matchesSheetName("Escala") }
        val escalistasSheet = workbook.sheets.firstOrNull { it.name.matchesSheetName("Escalistas") }

        if (escalaSheet == null) errors += "Aba Escala não encontrada."
        if (escalistasSheet == null) errors += "Aba Escalistas não encontrada."

        val dateTokens = escalaSheet?.scaleDateTokens().orEmpty()
        val yearAnalysis = resolveYear(workbook, dateTokens, confirmedStartYear)
        if (yearAnalysis is YearResolution.Invalid) errors += yearAnalysis.reason

        if (yearAnalysis is YearResolution.Ambiguous) {
            val partialDates = dateTokens.mapNotNull { it.partialDateToken() }
            return ScheduleImportPreview(
                fileName = workbook.fileName,
                sheetNames = workbook.sheetNames,
                collaborators = escalistasSheet?.readCollaborators().orEmpty(),
                selectedCollaborator = requestedCollaborator,
                daysRead = partialDates.size,
                warnings = listOf("A planilha possui dias e meses, mas não informa o ano de forma confiável."),
                errors = errors,
                summary = null,
                yearResolution = yearAnalysis,
                detectedPeriodStart = partialDates.firstOrNull()?.label,
                detectedPeriodEnd = partialDates.lastOrNull()?.label
            )
        }

        val resolvedYear = yearAnalysis as? YearResolution.Resolved
        val fullDateReferences = workbook.fullDateReferences()
        val startYear = resolvedYear?.startYear

        val collaborators = escalistasSheet?.readCollaborators().orEmpty()
        if (collaborators.isEmpty()) warnings += "Nenhum colaborador encontrado na aba Escalistas."

        val selected = requestedCollaborator
            ?.takeIf { requested -> collaborators.any { it.sameToken(requested) } }
            ?: collaborators.firstOrNull { it.sameToken(DefaultCollaborator) }
            ?: collaborators.firstOrNull()

        val statusByDate = if (escalistasSheet != null && selected != null) {
            escalistasSheet.readStatusesByDate(selected, warnings, startYear, fullDateReferences)
        } else {
            emptyMap()
        }

        val days = if (escalaSheet != null && selected != null) {
            escalaSheet.readScaleDays(selected, statusByDate, warnings, startYear, fullDateReferences)
        } else {
            emptyList()
        }

        if (days.isEmpty() && errors.isEmpty()) warnings += "Nenhum dia de escala foi lido para ${selected ?: "o colaborador"}."

        val summary = if (errors.isEmpty() && selected != null && days.isNotEmpty()) {
            buildSummary(workbook, selected, collaborators, days, warnings)
        } else {
            null
        }

        return ScheduleImportPreview(
            fileName = workbook.fileName,
            sheetNames = workbook.sheetNames,
            collaborators = collaborators,
            selectedCollaborator = selected,
            daysRead = days.size,
            warnings = warnings,
            errors = errors,
            summary = summary,
            yearResolution = yearAnalysis,
            detectedPeriodStart = days.firstNotNullOfOrNull { it.date }?.fullDateLabel(),
            detectedPeriodEnd = days.mapNotNull { it.date }.lastOrNull()?.fullDateLabel()
        )
    }

    /** Aba Escalistas: nomes vivem a partir da linha 2 (0-index), coluna 2. */
    private fun ImportedSheet.readCollaborators(): List<String> {
        return rows.drop(EscalistasHeaderRows)
            .mapNotNull { it.cell(EscalistasNameColumn).trim().takeIf { name -> name.isValidCollaboratorName() } }
            .distinctBy { it.normalizedKey() }
    }

    /**
     * Linha de datas fixa na linha 2 (0-index), colunas 3..32 (30 colunas),
     * igual ao parser oficial — nao auto-detecta mais a linha com mais
     * datas, para bater exatamente com o layout real da planilha.
     */
    private fun ImportedSheet.readStatusesByDate(
        collaborator: String,
        warnings: MutableList<String>,
        startYear: Int?,
        fullDateReferences: Map<Pair<Int, Int>, LabDate>
    ): Map<LabDate, String> {
        val dateRow = rows.getOrNull(EscalistasDateRowIndex) ?: return emptyMap()
        val resolver = DateSequenceResolver(startYear, fullDateReferences)
        val dateColumns = EscalistasStatusColumns.mapNotNull { column ->
            resolver.parse(dateRow.cell(column))?.let { column to it }
        }
        val collaboratorRow = rows.drop(EscalistasHeaderRows)
            .firstOrNull { it.cell(EscalistasNameColumn).sameToken(collaborator) }
            ?: return emptyMap()
        val statuses = linkedMapOf<LabDate, String>()

        dateColumns.forEach { (column, date) ->
            val status = collaboratorRow.cell(column).trim()
            if (status.isNotBlank()) statuses[date] = status
        }

        if (statuses.isEmpty()) warnings += "Não foram encontrados status diários para $collaborator na aba Escalistas."
        return statuses
    }

    /**
     * Aba Escala: 30 linhas fixas (2..31), data na coluna 0, observacoes na
     * coluna 6, turnos nas colunas 2..5 — igual ao parser oficial, sem
     * limite superior de linhas antes disso (agora com o mesmo range fixo).
     */
    private fun ImportedSheet.readScaleDays(
        collaborator: String,
        statusByDate: Map<LabDate, String>,
        warnings: MutableList<String>,
        startYear: Int?,
        fullDateReferences: Map<Pair<Int, Int>, LabDate>
    ): List<ShiftDay> {
        val resolver = DateSequenceResolver(startYear, fullDateReferences)
        val days = EscalaDayRows.mapNotNull { rowIndex ->
            val row = rows.getOrNull(rowIndex) ?: return@mapNotNull null
            val date = resolver.parse(row.cell(EscalaDateColumn)) ?: return@mapNotNull null
            val note = row.cell(EscalaNoteColumn).trim().takeIf { it.isNotBlank() }
            val membersByShift = ShiftColumns
                .mapNotNull { (column, type) ->
                    row.cell(column).teamMembers().takeIf { it.isNotEmpty() }?.let { type to it }
                }
                .toMap()
            val foundShift = ShiftColumns.firstNotNullOfOrNull { (column, type) ->
                val cellText = row.cell(column)
                if (cellText.containsCollaborator(collaborator)) ShiftCellMatch(type, cellText) else null
            }
            val status = statusByDate[date]
            val missingTurnWithWorkSequence = foundShift == null && status.isWorkSequenceNumber()
            val type = foundShift?.type ?: if (missingTurnWithWorkSequence) ShiftType.INCONSISTENCIA else status.toShiftTypeFallback()
            val sourceStatus = status ?: foundShift?.type?.shortLabel
            val finalNote = if (missingTurnWithWorkSequence) {
                "Status origem: $status. Colaborador não encontrado nos turnos da aba Escala."
            } else {
                note
            }
            val label = if (missingTurnWithWorkSequence) {
                "Trabalho sem turno localizado"
            } else {
                labelFor(type, sourceStatus)
            }

            ShiftDay(
                dayLabel = date.dayOfWeekShort(),
                dateLabel = date.dateLabel(),
                fullDateLabel = date.fullDateLabel(),
                type = type,
                date = date,
                teamMembers = foundShift?.rawCell.orEmpty().teamMembersExcluding(collaborator),
                membersByShift = membersByShift,
                sourceStatus = sourceStatus,
                note = finalNote,
                label = label
            )
        }.sortedBy { it.date }

        if (days.isEmpty()) warnings += "Nenhum dia de escala foi lido para $collaborator."
        return days
    }

    private fun buildSummary(
        workbook: ImportedWorkbook,
        selected: String,
        collaborators: List<String>,
        days: List<ShiftDay>,
        warnings: List<String>
    ): ScheduleSummary {
        val start = days.firstNotNullOfOrNull { it.date }
        val end = days.mapNotNull { it.date }.lastOrNull()
        val nextShift = days.firstOrNull { it.type.isWorkShift }
        val pause = nextShift?.type?.pauseLabel() ?: "Pausa não calculada"
        val pauseWindow = nextShift?.type?.pauseWindow()
        val pauseSuggestions = nextShift?.type?.pauseSuggestions().orEmpty()

        return ScheduleSummary(
            member = Member(
                email = "$selected@ici.tec.br",
                scaleName = selected,
                displayName = selected,
                teamId = "soc",
                role = MemberRole.ANALYST,
                active = true
            ),
            team = Team(teamId = "soc", name = "SOC", displayName = "SOC"),
            days = days,
            periodLabel = if (start != null && end != null) "${start.periodToken()} - ${end.periodToken()} ${end.year}" else "Período importado",
            pauseLabel = pause,
            pauseOffsetLabel = "1h após o início",
            pauseWindowStart = pauseWindow?.first ?: "--:--",
            pauseWindowEnd = pauseWindow?.second ?: "--:--",
            pauseSuggestions = pauseSuggestions,
            sourceFileName = workbook.fileName,
            sheetNames = workbook.sheetNames,
            collaborators = collaborators,
            warnings = (warnings + days.validationWarnings(selected)).distinct(),
            periodStart = start,
            periodEnd = end
        )
    }

    private fun List<ShiftDay>.validationWarnings(collaborator: String): List<String> {
        val alerts = mutableListOf<String>()
        val sortedDays = sortedBy { it.date }
        val inconsistencies = sortedDays.count { it.type == ShiftType.INCONSISTENCIA }
        val undefined = sortedDays.count { it.type == ShiftType.INDEFINIDO }
        var currentWorkSequence = 0
        var maxWorkSequence = 0

        sortedDays.forEach { day ->
            if (day.type.isWorkShift) {
                currentWorkSequence += 1
                maxWorkSequence = maxOf(maxWorkSequence, currentWorkSequence)
            } else {
                currentWorkSequence = 0
            }
        }

        if (inconsistencies > 0) {
            alerts += "$collaborator tem $inconsistencies dia(s) com divergência entre status e aba Escala."
        }
        if (undefined > 0) {
            alerts += "$collaborator tem $undefined dia(s) sem turno definido na leitura experimental."
        }
        if (maxWorkSequence > 6) {
            alerts += "$collaborator tem sequência de $maxWorkSequence dias trabalhados; revisar regra 6x1."
        }
        if (sortedDays.none { !it.type.isWorkShift }) {
            alerts += "Nenhum dia de descanso foi encontrado para $collaborator no período lido."
        }
        if (sortedDays.none { it.type.isWorkShift }) {
            alerts += "Nenhum turno de trabalho foi encontrado para $collaborator no período lido."
        }

        return alerts
    }

    // Horario sugerido de pausa (15 min), igual ao offset padrao do app real
    // (`NotificationPreferences.offsetMinutesAfterShiftStart = 60`): inicio
    // do turno + 60min ate +75min.
    private fun ShiftType.pauseLabel(): String = when (this) {
        ShiftType.MADRUGADA -> "02:00 - 02:15"
        ShiftType.MANHA -> "08:00 - 08:15"
        ShiftType.TARDE -> "14:00 - 14:15"
        ShiftType.NOITE -> "20:00 - 20:15"
        ShiftType.COMERCIAL -> "09:00 - 09:15"
        else -> "Pausa não calculada"
    }

    // Janela permitida para a pausa, igual ao `getPauseWindow()` do app
    // real (`PauseWindow.kt`): inicio do turno + 120min ate +285min (165
    // minutos de janela), fixa por tipo de turno.
    private fun ShiftType.pauseWindow(): Pair<String, String>? = when (this) {
        ShiftType.MADRUGADA -> "03:00" to "05:45"
        ShiftType.MANHA -> "09:00" to "11:45"
        ShiftType.TARDE -> "15:00" to "17:45"
        ShiftType.NOITE -> "21:00" to "23:45"
        ShiftType.COMERCIAL -> "10:00" to "12:45"
        else -> null
    }

    // Horarios sugeridos dentro da janela permitida, a cada 30min, igual
    // ao `suggestedPauseTimes()` do app real (6 opcoes por turno).
    private fun ShiftType.pauseSuggestions(): List<String> = when (this) {
        ShiftType.MADRUGADA -> listOf("03:00", "03:30", "04:00", "04:30", "05:00", "05:30")
        ShiftType.MANHA -> listOf("09:00", "09:30", "10:00", "10:30", "11:00", "11:30")
        ShiftType.TARDE -> listOf("15:00", "15:30", "16:00", "16:30", "17:00", "17:30")
        ShiftType.NOITE -> listOf("21:00", "21:30", "22:00", "22:30", "23:00", "23:30")
        ShiftType.COMERCIAL -> listOf("10:00", "10:30", "11:00", "11:30", "12:00", "12:30")
        else -> emptyList()
    }

    private data class ShiftCellMatch(val type: ShiftType, val rawCell: String)

    private fun String?.toShiftTypeFallback(): ShiftType {
        return when (this?.trim()?.uppercase()) {
            "DF", "DU", "FOLGA" -> ShiftType.FOLGA
            "X" -> ShiftType.FERIAS
            "BH" -> ShiftType.BH
            "AN" -> ShiftType.ANIVERSARIO
            "HE" -> ShiftType.HORA_EXTRA
            "#" -> ShiftType.AFASTAMENTO
            null, "" -> ShiftType.INDEFINIDO
            else -> ShiftType.INDEFINIDO
        }
    }

    /**
     * Rotulo apresentavel por tipo, igual ao `labelFor()` do parser oficial
     * — distinto de `ShiftType.label` para alguns tipos (BH, Aniversario,
     * Folga com status de origem).
     */
    private fun labelFor(type: ShiftType, sourceStatus: String?): String {
        return when (type) {
            ShiftType.FOLGA -> if (sourceStatus.isNullOrBlank()) "Folga" else "Folga / $sourceStatus"
            ShiftType.FERIAS -> "Férias"
            ShiftType.BH -> "Banco de horas"
            ShiftType.ANIVERSARIO -> "Folga aniversário"
            ShiftType.HORA_EXTRA -> "Hora extra"
            ShiftType.AFASTAMENTO -> "Afastamento"
            ShiftType.INDEFINIDO -> "Indefinido"
            else -> type.label
        }
    }

    private fun String?.isWorkSequenceNumber(): Boolean = this?.trim()?.matches(Regex("[1-6]")) == true

    /** Colunas de turno usam 4 separadores para achar o colaborador. */
    private fun String.containsCollaborator(collaborator: String): Boolean {
        return split('/', '\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any { it.sameToken(collaborator) }
    }

    /**
     * Extrai a equipe do turno encontrado usando so 3 separadores (sem
     * `;`) — igual ao parser oficial, que usa um conjunto diferente do
     * usado para achar o colaborador (`containsCollaborator`, 4
     * separadores). Assimetria intencional, replicada aqui.
     */
    private fun String.teamMembersExcluding(collaborator: String): List<String> {
        return split('/', '\n', ',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filterNot { it.sameToken(collaborator) }
            .distinctBy { it.normalizedKey() }
    }

    private fun String.teamMembers(): List<String> {
        return split('/', '\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizedKey() }
    }

    private fun String.parseFullDateToken(): LabDate? {
        val token = trim().split(Regex("""\s+""")).firstOrNull().orEmpty()
        val parts = token.split('/')
        if (parts.size != 3) return null
        val day = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val year = parts.getOrNull(2)?.toIntOrNull() ?: return null
        if (month !in 1..12 || year !in 2000..2100) return null
        if (day !in 1..LabDate.monthLength(year, month)) return null
        return LabDate(year, month, day)
    }

    private data class PartialDate(val day: Int, val month: Int) {
        val label: String get() = "${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')}"
    }

    private fun String.partialDateToken(): PartialDate? {
        val token = trim().split(Regex("""\s+""")).firstOrNull().orEmpty()
        val parts = token.split('/')
        if (parts.size !in 2..3) return null
        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..31) return null
        return PartialDate(day, month)
    }

    private fun ImportedSheet.scaleDateTokens(): List<String> = EscalaDayRows.mapNotNull { rowIndex ->
        rows.getOrNull(rowIndex)?.cell(EscalaDateColumn)?.takeIf { it.partialDateToken() != null }
    }

    private fun resolveYear(
        workbook: ImportedWorkbook,
        orderedScaleTokens: List<String>,
        confirmedStartYear: Int?
    ): YearResolution {
        if (confirmedStartYear != null) {
            return if (confirmedStartYear in 2000..2100) {
                YearResolution.Resolved(confirmedStartYear, YearResolutionSource.USER_CONFIRMED)
            } else {
                YearResolution.Invalid("O ano confirmado deve estar entre 2000 e 2100.")
            }
        }

        val fullDates = workbook.sheets.flatMap { sheet -> sheet.rows.flatten().mapNotNull { it.parseFullDateToken() } }
        if (fullDates.isNotEmpty()) {
            val firstPartialMonth = orderedScaleTokens.firstOrNull()?.partialDateToken()?.month
            val firstFull = fullDates.first()
            val startYear = if (firstPartialMonth != null && firstPartialMonth > firstFull.month) firstFull.year - 1 else firstFull.year
            return YearResolution.Resolved(startYear, YearResolutionSource.FULL_DATE_IN_WORKBOOK)
        }

        if (orderedScaleTokens.any { it.hasExplicitYearToken() }) {
            return YearResolution.Invalid("A aba Escala contém uma data completa inválida.")
        }

        val fileYear = ExplicitYearRegex.find(workbook.fileName)?.value?.toIntOrNull()
        if (fileYear != null) return YearResolution.Resolved(fileYear, YearResolutionSource.FILE_NAME)

        return if (orderedScaleTokens.isNotEmpty()) {
            YearResolution.Ambiguous(
                evidence = listOf("Datas encontradas somente no formato dia/mês.")
            )
        } else {
            YearResolution.Invalid("Nenhuma data válida foi encontrada na aba Escala.")
        }
    }

    private fun String.hasExplicitYearToken(): Boolean {
        val token = trim().split(Regex("""\s+""")).firstOrNull().orEmpty()
        val parts = token.split('/')
        return parts.size == 3 && parts[2].toIntOrNull() != null
    }

    private fun ImportedWorkbook.fullDateReferences(): Map<Pair<Int, Int>, LabDate> = sheets
        .flatMap { sheet -> sheet.rows.flatten().mapNotNull { it.parseFullDateToken() } }
        .groupBy { it.day to it.month }
        .mapNotNull { (key, dates) -> dates.distinct().singleOrNull()?.let { key to it } }
        .toMap()

    private class DateSequenceResolver(
        private val initialYear: Int?,
        private val fullDateReferences: Map<Pair<Int, Int>, LabDate>
    ) {
        private var currentYear = initialYear
        private var previousMonth: Int? = null

        fun parse(value: String): LabDate? {
            value.parseFullDateToken()?.let { full ->
                currentYear = full.year
                previousMonth = full.month
                return full
            }
            val partial = value.partialDateToken() ?: return null
            fullDateReferences[partial.day to partial.month]?.let { reference ->
                currentYear = reference.year
                previousMonth = reference.month
                return reference
            }
            var year = currentYear ?: return null
            if (previousMonth == 12 && partial.month == 1) year += 1
            if (partial.day !in 1..LabDate.monthLength(year, partial.month)) return null
            currentYear = year
            previousMonth = partial.month
            return LabDate(year, partial.month, partial.day)
        }
    }

    private fun String.isValidCollaboratorName(): Boolean {
        val clean = trim()
        if (clean.isBlank()) return false
        if (clean.contains('/')) return false
        if (clean.any { it.isDigit() }) return false
        if (clean.partialDateToken() != null) return false
        val blocked = setOf(
            "diames", "diasemana", "data", "colaborador", "colaboradores", "escalista", "escalistas",
            "tecnico", "tecnica", "analista", "nome", "turno", "escala"
        )
        val key = clean.normalizedKey()
        if (key in blocked) return false
        return clean.matches(Regex("[A-Za-zÀ-ÿ._-]{3,}"))
    }

    private fun List<String>.cell(index: Int): String = getOrNull(index).orEmpty()

    private fun String.matchesSheetName(expected: String): Boolean = normalizedKey() == expected.normalizedKey()

    private fun String.sameToken(other: String): Boolean = normalizedKey() == other.normalizedKey()

    private fun String.normalizedKey(): String {
        return trim()
            .lowercase()
            .map { accentMap[it] ?: it }
            .joinToString("")
            .replace(Regex("""\s+"""), "")
    }

    private const val DefaultCollaborator = "lvergani"
    private val ExplicitYearRegex = Regex("(?<!\\d)(20\\d{2})(?!\\d)")

    // Layout fixo da aba Escalistas (igual ao parser oficial): header nas
    // linhas 0-1, dados a partir da linha 2; a propria linha 2 tambem
    // carrega as datas (colunas 3..32) — nao conflita com nomes porque
    // "colaborador"/"nome" estao no blocklist de isValidCollaboratorName.
    private const val EscalistasHeaderRows = 2
    private const val EscalistasDateRowIndex = 2
    private const val EscalistasNameColumn = 2
    private val EscalistasStatusColumns = 3..32

    // Layout fixo da aba Escala (igual ao parser oficial): 30 linhas (um
    // mes), data na coluna 0, observacoes na coluna 6.
    private val EscalaDayRows = 2..31
    private const val EscalaDateColumn = 0
    private const val EscalaNoteColumn = 6
    private val ShiftColumns = listOf(
        2 to ShiftType.MADRUGADA,
        3 to ShiftType.MANHA,
        4 to ShiftType.TARDE,
        5 to ShiftType.NOITE
    )
    private val accentMap = mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c'
    )
}
