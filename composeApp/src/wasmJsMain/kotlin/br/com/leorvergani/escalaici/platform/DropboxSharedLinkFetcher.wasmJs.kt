package br.com.leorvergani.escalaici.platform

import br.com.leorvergani.escalaici.model.DropboxAuthConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
actual suspend fun downloadDropboxSharedLink(sharedLinkUrl: String): ByteArray {
    val base64 = suspendCancellableCoroutine<String> { continuation ->
        callDropboxFetch(
            sharedLinkUrl,
            DropboxAuthConfig.APP_KEY,
            DropboxAuthConfig.REDIRECT_URI,
            DropboxAuthConfig.SCOPE
        ) { status, payload ->
            if (!continuation.isActive) return@callDropboxFetch
            if (status == "success") {
                continuation.resumeWith(Result.success(payload))
            } else {
                continuation.resumeWith(Result.failure(RuntimeException(payload)))
            }
        }
    }
    return Base64.Default.decode(base64)
}

private fun callDropboxFetch(
    url: String,
    appKey: String,
    redirectUri: String,
    scope: String,
    callback: (String, String) -> Unit
) {
    js("globalThis.escalaIciDropboxFetchSharedLinkBytesBase64(url, appKey, redirectUri, scope, callback)")
}
