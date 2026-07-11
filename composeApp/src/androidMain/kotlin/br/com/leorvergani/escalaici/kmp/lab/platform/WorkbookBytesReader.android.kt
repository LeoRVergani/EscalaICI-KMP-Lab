package br.com.leorvergani.escalaici.kmp.lab.platform

import br.com.leorvergani.escalaici.kmp.lab.model.WorkbookImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun readWorkbookFromBytes(fileName: String, bytes: ByteArray): WorkbookImportResult {
    return withContext(Dispatchers.IO) {
        parseWorkbookBytesAndroid(fileName, bytes)
    }
}
