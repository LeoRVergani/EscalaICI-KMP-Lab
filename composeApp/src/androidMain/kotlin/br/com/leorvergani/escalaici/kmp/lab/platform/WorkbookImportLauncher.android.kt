package br.com.leorvergani.escalaici.kmp.lab.platform

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedSheet
import br.com.leorvergani.escalaici.kmp.lab.model.ImportedWorkbook
import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayInputStream
import java.util.Locale

@Composable
actual fun rememberWorkbookImportLauncher(
    onResult: (WorkbookImportResult) -> Unit
): WorkbookImportLauncher {
    val context = LocalContext.current
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        currentOnResult.value(readWorkbook(context, uri))
    }

    return remember(launcher) {
        object : WorkbookImportLauncher {
            override fun launch() {
                launcher.launch(
                    arrayOf(
                        "application/vnd.ms-excel",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/octet-stream"
                    )
                )
            }
        }
    }
}

private fun readWorkbook(context: Context, uri: Uri): WorkbookImportResult {
    val fileName = context.displayName(uri) ?: "planilha.xls"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        ?: return WorkbookImportResult.Failure(fileName, "Não foi possível abrir o arquivo selecionado.")
    return parseWorkbookBytesAndroid(fileName, bytes)
}

/** Compartilhado pelo seletor de arquivo local e pelo download remoto (Dropbox). */
internal fun parseWorkbookBytesAndroid(fileName: String, bytes: ByteArray): WorkbookImportResult {
    return runCatching {
        val extension = fileName.substringAfterLast('.', "xls").lowercase(Locale.ROOT)
        val workbook = when (extension) {
            "xls" -> HSSFWorkbook(ByteArrayInputStream(bytes))
            "xlsx" -> XSSFWorkbook(ByteArrayInputStream(bytes))
            else -> return WorkbookImportResult.Failure(fileName, "Formato inválido. Selecione um arquivo .xls ou .xlsx.")
        }
        workbook.use { opened ->
            WorkbookImportResult.Success(opened.toImportedWorkbook(fileName))
        }
    }.getOrElse { error ->
        WorkbookImportResult.Failure(fileName, "Não foi possível ler a planilha: ${error.message ?: error::class.simpleName}")
    }
}

private fun Workbook.toImportedWorkbook(fileName: String): ImportedWorkbook {
    val formatter = DataFormatter(Locale.forLanguageTag("pt-BR"))
    val sheets = List(numberOfSheets) { sheetIndex ->
        val sheet = getSheetAt(sheetIndex)
        val rows = (0..sheet.lastRowNum).map { rowIndex ->
            val row = sheet.getRow(rowIndex)
            val lastCell = row?.lastCellNum?.toInt()?.coerceAtLeast(0) ?: 0
            (0 until lastCell).map { cellIndex ->
                row?.getCell(cellIndex)?.let(formatter::formatCellValue).orEmpty()
            }
        }
        ImportedSheet(name = getSheetName(sheetIndex), rows = rows)
    }
    return ImportedWorkbook(fileName = fileName, sheets = sheets)
}

private fun Context.displayName(uri: Uri): String? {
    return contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }
}
