package br.com.leorvergani.escalaici.model

/**
 * Parser proprio do laboratorio (codigo novo, nao copiado) para o
 * relatorio real de plantao — separado do `LabWorkbookParser` (escala
 * 6x1), igual ao app real (`PlantaoWorkbookParser.kt`, `EscalaSOC`, so
 * leitura): "Totalmente separado do ScaleWorkbookParser: o Plantao
 * trabalha com intervalos de data/hora, nao com dias da escala 6x1".
 *
 * Regras portadas exatamente do parser oficial (mesmos nomes de coluna,
 * mesma regex, mesmas validacoes de linha, mesma mensagem de erro):
 * - Cabecalho: primeira linha (qualquer aba) com colunas "plantonista",
 *   "datainicio"/"inicio" e "datafim"/"fim" (normalizado sem acento/
 *   case/espaco).
 * - Data+hora: regex `(\d{1,2})/(\d{1,2})/(\d{2,4})\s*-?\s*(\d{1,2}):(\d{2})`
 *   (aceita "-" opcional entre data e hora; exige hora, sem fallback
 *   00:00 como o parser de escala tem para datas puras).
 * - Linha com plantonista+datas todos vazios: pulada em silencio.
 * - Linha com só parte dos 3 campos preenchida: aviso "Linha N: plantão
 *   incompleto ignorado.".
 * - Fim <= inicio: aviso "Linha N: data final menor ou igual à inicial.".
 * - Nenhum plantao valido encontrado: erro (nao aviso) "Não encontrei
 *   plantões no formato esperado: Plantonista Segurança, Data Inicio e
 *   Data Fim." — igual a excecao lancada pelo parser oficial.
 *
 * Limitacao conhecida (mesma do `LabWorkbookParser`, nao corrigida nesta
 * fase): sem atalho para celulas de data POI cruas — o parser oficial usa
 * `DateUtil.getLocalDateTime` direto quando a celula e um numero de data
 * real; aqui (Android e Web) tudo passa por texto formatado
 * (`DataFormatter`/SheetJS) antes de chegar neste parser.
 */
object PlantaoWorkbookParser {
    fun parse(workbook: ImportedWorkbook): PlantaoImportResult {
        val warnings = mutableListOf<String>()
        val shifts = mutableListOf<ParsedShift>()

        workbook.sheets.forEach { sheet ->
            val header = sheet.rows.findHeader() ?: return@forEach

            sheet.rows.drop(header.rowIndex + 1).forEachIndexed { offset, row ->
                val rowIndex = header.rowIndex + 1 + offset
                val lineNumber = rowIndex + 1
                val plantonista = row.getOrNull(header.nameColumn)?.trim()?.replace(Regex("""\s+"""), " ").orEmpty()
                val start = row.getOrNull(header.startColumn)?.parseDateTimeCell()
                val end = row.getOrNull(header.endColumn)?.parseDateTimeCell()

                if (plantonista.isBlank() && start == null && end == null) return@forEachIndexed

                if (plantonista.isBlank() || start == null || end == null) {
                    warnings += "Linha $lineNumber: plantão incompleto ignorado."
                    return@forEachIndexed
                }
                if (end.epochMinutes <= start.epochMinutes) {
                    warnings += "Linha $lineNumber: data final menor ou igual à inicial."
                    return@forEachIndexed
                }

                shifts += ParsedShift(plantonista = plantonista, start = start, end = end, source = sheet.name)
            }
        }

        val ordered = shifts.sortedWith(compareBy<ParsedShift> { it.start.epochMinutes }.thenBy { it.plantonista.lowercase() })
        if (ordered.isEmpty()) {
            return PlantaoImportResult(
                fileName = workbook.fileName,
                assignments = emptyList(),
                warnings = warnings,
                error = "Não encontrei plantões no formato esperado: Plantonista Segurança, Data Inicio e Data Fim."
            )
        }

        val assignments = ordered.mapIndexed { index, shift ->
            OnCallAssignment(
                id = "${shift.plantonista.normalizedKey()}:${shift.start.epochMinutes}:${shift.end.epochMinutes}:$index",
                periodId = "plantao-importado",
                teamId = "soc",
                memberId = shift.plantonista,
                memberName = shift.plantonista,
                date = shift.start.isoDate,
                startDate = shift.start.isoDate,
                endDate = shift.end.isoDate,
                startTime = shift.start.time,
                endTime = shift.end.time,
                status = OnCallStatus.SCHEDULED
            )
        }

        return PlantaoImportResult(fileName = workbook.fileName, assignments = assignments, warnings = warnings, error = null)
    }

    private data class HeaderColumns(val rowIndex: Int, val nameColumn: Int, val startColumn: Int, val endColumn: Int)

    private fun List<List<String>>.findHeader(): HeaderColumns? {
        forEachIndexed { rowIndex, row ->
            var nameColumn = -1
            var startColumn = -1
            var endColumn = -1
            row.forEachIndexed { columnIndex, cell ->
                val key = cell.normalizedKey()
                if (key.isBlank()) return@forEachIndexed
                if (nameColumn < 0 && key.contains("plantonista")) nameColumn = columnIndex
                if (startColumn < 0 && (key.contains("datainicio") || key.contains("inicio"))) startColumn = columnIndex
                if (endColumn < 0 && (key.contains("datafim") || key.contains("fim"))) endColumn = columnIndex
            }
            if (nameColumn >= 0 && startColumn >= 0 && endColumn >= 0) {
                return HeaderColumns(rowIndex, nameColumn, startColumn, endColumn)
            }
        }
        return null
    }

    private data class ParsedMoment(val isoDate: String, val time: String, val epochMinutes: Long)
    private data class ParsedShift(val plantonista: String, val start: ParsedMoment, val end: ParsedMoment, val source: String)

    /** Mesma regex do parser oficial: aceita "-" opcional entre data e hora, hora obrigatória. */
    private fun String.parseDateTimeCell(): ParsedMoment? {
        val match = DateTimeRegex.find(trim()) ?: return null
        val (dayStr, monthStr, yearStr, hourStr, minuteStr) = match.destructured
        val day = dayStr.toIntOrNull() ?: return null
        val month = monthStr.toIntOrNull() ?: return null
        var year = yearStr.toIntOrNull() ?: return null
        if (year < 100) year += 2000
        val hour = hourStr.toIntOrNull() ?: return null
        val minute = minuteStr.toIntOrNull() ?: return null
        if (month !in 1..12 || day !in 1..LabDate.monthLength(year, month) || hour !in 0..23 || minute !in 0..59) return null

        val date = LabDate(year, month, day)
        val time = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
        val iso = "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
        val epochMinutes = date.epochDay().toLong() * 24 * 60 + hour * 60 + minute
        return ParsedMoment(isoDate = iso, time = time, epochMinutes = epochMinutes)
    }

    private fun String.normalizedKey(): String {
        return trim()
            .lowercase()
            .map { AccentMap[it] ?: it }
            .joinToString("")
            .replace(Regex("""\s+"""), "")
    }

    private val AccentMap = mapOf(
        'á' to 'a', 'à' to 'a', 'â' to 'a', 'ã' to 'a', 'ä' to 'a',
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'í' to 'i', 'ì' to 'i', 'î' to 'i', 'ï' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ô' to 'o', 'õ' to 'o', 'ö' to 'o',
        'ú' to 'u', 'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c'
    )

    private val DateTimeRegex = Regex("""(\d{1,2})/(\d{1,2})/(\d{2,4})\s*-?\s*(\d{1,2}):(\d{2})""")
}

data class PlantaoImportResult(
    val fileName: String,
    val assignments: List<OnCallAssignment>,
    val warnings: List<String>,
    val error: String? = null
)
