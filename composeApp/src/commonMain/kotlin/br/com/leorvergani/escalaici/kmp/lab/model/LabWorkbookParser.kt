package br.com.leorvergani.escalaici.kmp.lab.model

object LabWorkbookParser {
    fun parse(workbook: ImportedWorkbook, requestedCollaborator: String? = DefaultCollaborator): ScheduleImportPreview {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        val escalaSheet = workbook.sheets.firstOrNull { it.name.matchesSheetName("Escala") }
        val escalistasSheet = workbook.sheets.firstOrNull { it.name.matchesSheetName("Escalistas") }

        if (escalaSheet == null) errors += "Aba Escala não encontrada."
        if (escalistasSheet == null) errors += "Aba Escalistas não encontrada."

        val collaborators = escalistasSheet?.readCollaborators().orEmpty()
        if (collaborators.isEmpty()) warnings += "Nenhum colaborador encontrado na aba Escalistas."

        val selected = requestedCollaborator
            ?.takeIf { requested -> collaborators.any { it.sameToken(requested) } }
            ?: collaborators.firstOrNull { it.sameToken(DefaultCollaborator) }
            ?: collaborators.firstOrNull()

        val statusByDate = if (escalistasSheet != null && selected != null) {
            escalistasSheet.readStatusesByDate(selected, warnings)
        } else {
            emptyMap()
        }

        val days = if (escalaSheet != null && selected != null) {
            escalaSheet.readScaleDays(selected, statusByDate, warnings)
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
            summary = summary
        )
    }

    private fun ImportedSheet.readCollaborators(): List<String> {
        return rows
            .mapNotNull { it.cell(2).trim().takeIf { name -> name.isValidCollaboratorName() } }
            .distinctBy { it.normalizedKey() }
    }

    private fun ImportedSheet.readStatusesByDate(
        collaborator: String,
        warnings: MutableList<String>
    ): Map<LabDate, String> {
        val dateRow = rows.maxByOrNull { row -> (3 until row.size).count { row.cell(it).parseDateToken() != null } }
            ?: return emptyMap()
        val dateColumns = dateRow.mapIndexedNotNull { index, value ->
            if (index < 3) null else value.parseDateToken()?.let { index to it }
        }
        val collaboratorRow = rows.firstOrNull { it.cell(2).sameToken(collaborator) } ?: return emptyMap()
        val statuses = linkedMapOf<LabDate, String>()

        dateColumns.forEach { (column, date) ->
            val status = collaboratorRow.cell(column).trim()
            if (status.isNotBlank()) statuses[date] = status
        }

        if (statuses.isEmpty()) warnings += "Não foram encontrados status diários para $collaborator na aba Escalistas."
        return statuses
    }

    private fun ImportedSheet.readScaleDays(
        collaborator: String,
        statusByDate: Map<LabDate, String>,
        warnings: MutableList<String>
    ): List<ShiftDay> {
        val days = rows.mapNotNull { row ->
            val date = row.cell(0).parseDateToken() ?: return@mapNotNull null
            val note = row.cell(6).trim().takeIf { it.isNotBlank() }
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

            ShiftDay(
                dayLabel = date.dayOfWeekShort(),
                dateLabel = date.dateLabel(),
                fullDateLabel = date.fullDateLabel(),
                type = type,
                date = date,
                teamMembers = foundShift?.rawCell.orEmpty().teamMembersExcluding(collaborator),
                membersByShift = membersByShift,
                sourceStatus = sourceStatus,
                note = finalNote
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

        return ScheduleSummary(
            member = Member(
                email = "$selected@ici.tec.br",
                scaleName = selected,
                displayName = selected
            ),
            team = Team(teamId = "soc", name = "SOC"),
            days = days,
            periodLabel = if (start != null && end != null) "${start.periodToken()} - ${end.periodToken()} ${end.year}" else "Período importado",
            pauseLabel = pause,
            pauseOffsetLabel = "1h após o início",
            sourceFileName = workbook.fileName,
            sheetNames = workbook.sheetNames,
            collaborators = collaborators,
            warnings = (warnings + days.validationWarnings(selected)).distinct()
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

    private fun ShiftType.pauseLabel(): String = when (this) {
        ShiftType.MADRUGADA -> "02:00 - 02:15"
        ShiftType.MANHA -> "08:00 - 08:15"
        ShiftType.TARDE -> "14:00 - 14:15"
        ShiftType.NOITE -> "20:00 - 20:15"
        else -> "Pausa não calculada"
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

    private fun String?.isWorkSequenceNumber(): Boolean = this?.trim()?.matches(Regex("[1-6]")) == true

    private fun String.containsCollaborator(collaborator: String): Boolean {
        return split('/', '\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any { it.sameToken(collaborator) }
    }

    private fun String.teamMembersExcluding(collaborator: String): List<String> {
        return teamMembers()
            .filterNot { it.sameToken(collaborator) }
            .distinctBy { it.normalizedKey() }
    }

    private fun String.teamMembers(): List<String> {
        return split('/', '\n', ',', ';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.normalizedKey() }
    }

    private fun String.parseDateToken(): LabDate? {
        val token = trim().split(Regex("""\s+""")).firstOrNull().orEmpty()
        val parts = token.split('/')
        if (parts.size !in 2..3) return null
        val day = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return null
        val year = parts.getOrNull(2)?.toIntOrNull() ?: YearFallback
        if (day !in 1..31 || month !in 1..12 || year !in 2000..2100) return null
        return LabDate(year, month, day)
    }

    private fun String.isValidCollaboratorName(): Boolean {
        val clean = trim()
        if (clean.isBlank()) return false
        if (clean.contains('/')) return false
        if (clean.any { it.isDigit() }) return false
        if (clean.parseDateToken() != null) return false
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
    private const val YearFallback = 2026
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
