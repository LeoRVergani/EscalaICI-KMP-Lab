package br.com.leorvergani.escalaici.auth

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.JsAny
import kotlin.js.JsBoolean
import kotlin.js.JsString
import kotlin.js.Promise
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

private fun jsInit(tenantId: String, clientId: String, redirectUri: String): Promise<JsBoolean> =
    js("globalThis.escalaIciMsalInit(tenantId, clientId, redirectUri)")

private fun jsGetActiveIdentityJson(): String? =
    js("globalThis.escalaIciMsalGetActiveIdentityJson()")

private fun jsAcquireTokenSilent(scopesCsv: String): Promise<JsString> =
    js("globalThis.escalaIciMsalAcquireTokenSilent(scopesCsv)")

private fun jsLoginPopup(scopesCsv: String): Promise<JsString> =
    js("globalThis.escalaIciMsalLoginPopup(scopesCsv)")

private fun jsLogoutPopup(): Promise<JsBoolean> =
    js("globalThis.escalaIciMsalLogoutPopup()")

external interface JsRejection : JsAny {
    val kind: String?
    val detail: String?
}

suspend fun <T : JsAny?> Promise<T>.await(): T =
    suspendCancellableCoroutine { continuation ->
        then(
            onFulfilled = { value ->
                continuation.resumeIfActive(value)
                value
            },
            onRejected = { reason ->
                val rejection = reason as? JsRejection
                continuation.resumeWithExceptionIfActive(
                    MsalWebRejection(
                        kind = rejection?.kind ?: "unknown",
                        detail = rejection?.detail ?: "Falha ao autenticar com a conta corporativa.",
                    )
                )
                null
            },
        )
    }

suspend fun msalWebInit(config: MsalWebConfig) {
    jsInit(config.tenantId, config.clientId, config.redirectUri).await()
}

fun msalWebGetActiveIdentityJson(): String? = jsGetActiveIdentityJson()

suspend fun msalWebAcquireTokenSilent(scopesCsv: String): String =
    jsAcquireTokenSilent(scopesCsv).await().toString()

suspend fun msalWebLoginPopup(scopesCsv: String): String =
    jsLoginPopup(scopesCsv).await().toString()

suspend fun msalWebLogoutPopup() {
    jsLogoutPopup().await()
}

private fun <T> CancellableContinuation<T>.resumeIfActive(value: T) {
    if (isActive) {
        resume(value)
    }
}

private fun <T> CancellableContinuation<T>.resumeWithExceptionIfActive(exception: Throwable) {
    if (isActive) {
        resumeWithException(exception)
    }
}
