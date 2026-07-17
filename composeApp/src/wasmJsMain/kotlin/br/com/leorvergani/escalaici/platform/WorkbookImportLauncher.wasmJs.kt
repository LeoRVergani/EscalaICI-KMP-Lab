package br.com.leorvergani.escalaici.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import br.com.leorvergani.escalaici.model.ImportedSheet
import br.com.leorvergani.escalaici.model.ImportedWorkbook
import br.com.leorvergani.escalaici.model.WorkbookImportResult

@Composable
actual fun rememberWorkbookImportLauncher(
    onResult: (WorkbookImportResult) -> Unit
): WorkbookImportLauncher {
    val currentOnResult = rememberUpdatedState(onResult)
    return remember {
        object : WorkbookImportLauncher {
            override fun launch() {
                openWorkbookPicker { status, fileName, payload ->
                    currentOnResult.value(payload.toImportResult(status, fileName))
                }
            }
        }
    }
}

private fun openWorkbookPicker(callback: (String, String, String) -> Unit) {
    js("globalThis.escalaIciOpenWorkbookPicker(callback)")
}

internal fun String.toImportResult(status: String, fileName: String): WorkbookImportResult {
    if (status != "success") {
        return WorkbookImportResult.Failure(fileName.takeIf { it.isNotBlank() }, this.ifBlank { "Não foi possível ler a planilha." })
    }
    return runCatching {
        WorkbookImportResult.Success(parseWorkbookPayload(fileName, this))
    }.getOrElse { error ->
        WorkbookImportResult.Failure(fileName, "Não foi possível interpretar a planilha: ${error.message ?: error::class.simpleName}")
    }
}

private fun parseWorkbookPayload(fileName: String, payload: String): ImportedWorkbook {
    val sheets = payload
        .split(SheetSeparator)
        .filter { it.isNotBlank() }
        .map { sheetPayload ->
            val parts = sheetPayload.split(CellSeparator)
            val name = parts.firstOrNull().orEmpty().percentDecode()
            val rowPayload = parts.drop(1).joinToString(CellSeparator)
            val rows = rowPayload
                .split(RowSeparator)
                .filter { it.isNotEmpty() }
                .map { row ->
                    row.split(CellSeparator).map { it.percentDecode() }
                }
            ImportedSheet(name = name, rows = rows)
        }
    return ImportedWorkbook(fileName = fileName, sheets = sheets)
}

private fun String.percentDecode(): String {
    val bytes = mutableListOf<Byte>()
    var index = 0
    while (index < length) {
        val char = this[index]
        if (char == '%' && index + 2 < length) {
            val hex = substring(index + 1, index + 3)
            bytes += hex.toInt(16).toByte()
            index += 3
        } else {
            bytes += char.code.toByte()
            index += 1
        }
    }
    return bytes.toByteArray().decodeToString()
}

private const val SheetSeparator = "\u001D"
private const val RowSeparator = "\u001E"
private const val CellSeparator = "\u001F"
