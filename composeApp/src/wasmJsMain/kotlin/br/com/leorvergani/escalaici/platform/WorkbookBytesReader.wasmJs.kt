package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.WorkbookImportResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun readWorkbookFromBytes(fileName: String, bytes: ByteArray): WorkbookImportResult {
    val base64 = Base64.Default.encode(bytes)
    return suspendCancellableCoroutine { continuation ->
        callParseWorkbookBase64(base64, fileName) { status, name, payload ->
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(payload.toImportResult(status, name)))
            }
        }
    }
}

private fun callParseWorkbookBase64(base64: String, fileName: String, callback: (String, String, String) -> Unit) {
    js("globalThis.escalaIciParseWorkbookBase64(base64, fileName, callback)")
}
