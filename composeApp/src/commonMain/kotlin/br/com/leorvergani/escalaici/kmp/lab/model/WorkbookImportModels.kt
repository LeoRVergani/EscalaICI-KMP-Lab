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
    val summary: ScheduleSummary?
) {
    val canUseImportedData: Boolean
        get() = summary != null && errors.isEmpty()

    val statusLabel: String
        get() = when {
            errors.isNotEmpty() -> "Erro na leitura"
            warnings.isNotEmpty() -> "Lida com avisos"
            summary != null -> "Leitura concluída"
            else -> "Aguardando arquivo"
        }
}
