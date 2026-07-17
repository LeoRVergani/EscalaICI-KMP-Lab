package br.com.leorvergani.escalaici.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val TIMEOUT_MS = 15_000

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun downloadBytes(url: String): ByteArray {
    val base64 = suspendCancellableCoroutine<String> { continuation ->
        callDownloadBytesBase64(url, TIMEOUT_MS) { status, payload ->
            if (!continuation.isActive) return@callDownloadBytesBase64
            if (status == "success") {
                continuation.resumeWith(Result.success(payload))
            } else {
                continuation.resumeWith(Result.failure(RuntimeException(payload)))
            }
        }
    }
    return Base64.Default.decode(base64)
}

private fun callDownloadBytesBase64(url: String, timeoutMs: Int, callback: (String, String) -> Unit) {
    js("globalThis.escalaIciDownloadBytesBase64(url, timeoutMs, callback)")
}
