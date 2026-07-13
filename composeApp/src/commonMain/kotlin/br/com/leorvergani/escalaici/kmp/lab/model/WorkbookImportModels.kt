package br.com.leorvergani.escalaici.kmp.lab.model

data class ImportedWorkbook(
    val fileName: String,
    val sheets: List<ImportedSheet>
) {
    val sheetNames: List<String>
        get() = sheets.map { it.name }
}

data class ImportedSheet(
    val name: String,
    val rows: List<List<String>>
)

enum class YearResolutionSource {
    FULL_DATE_IN_WORKBOOK,
    WORKBOOK_METADATA,
    FILE_NAME,
    USER_CONFIRMED
}

sealed interface YearResolution {
    data class Resolved(
        /** Ano do primeiro dia do período. */
        val startYear: Int,
        val source: YearResolutionSource
    ) : YearResolution

    data class Ambiguous(
        val suggestedYear: Int? = null,
        val evidence: List<String> = emptyList()
    ) : YearResolution

    data class Invalid(val reason: String) : YearResolution
}

sealed interface WorkbookImportResult {
    data class Success(val workbook: ImportedWorkbook) : WorkbookImportResult
    data class Failure(val fileName: String?, val message: String) : WorkbookImportResult
}

data class ScheduleImportPreview(
    val fileName: String,
    val sheetNames: List<String>,
    val collaborators: List<String>,
    val selectedCollaborator: String?,
    val daysRead: Int,
    val warnings: List<String>,
    val errors: List<String>,
    val summary: ScheduleSummary?,
    val yearResolution: YearResolution = YearResolution.Invalid("Ano ainda não analisado."),
    val detectedPeriodStart: String? = null,
    val detectedPeriodEnd: String? = null
) {
    val canUseImportedData: Boolean
        get() = summary != null && errors.isEmpty() && yearResolution is YearResolution.Resolved

    val statusLabel: String
        get() = when {
            errors.isNotEmpty() -> "Erro na leitura"
            yearResolution is YearResolution.Ambiguous -> "Confirmar ano"
            warnings.isNotEmpty() -> "Lida com avisos"
            summary != null -> "Leitura concluída"
            else -> "Aguardando arquivo"
        }
}
