package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.WorkbookImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun readWorkbookFromBytes(fileName: String, bytes: ByteArray): WorkbookImportResult {
    return withContext(Dispatchers.IO) {
        parseWorkbookBytesAndroid(fileName, bytes)
    }
}
