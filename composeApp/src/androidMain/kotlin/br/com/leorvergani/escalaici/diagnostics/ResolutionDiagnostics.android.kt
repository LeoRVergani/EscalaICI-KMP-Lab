package br.com.leorvergani.escalaici.diagnostics

import android.util.Log

private const val TAG = "EscalaIciResolution"

actual fun logResolutionFailure(diagnostic: ResolutionFailureDiagnostic) {
    Log.w(
        TAG,
        "[Android] step=${diagnostic.step} workspace=${diagnostic.workspaceId} " +
            "cause=${diagnostic.cause} exception=${diagnostic.exceptionType} " +
            "http=${diagnostic.httpStatus ?: "-"} msg=${diagnostic.sanitizedMessage}"
    )
}

actual fun logResolutionTrace(message: String) {
    Log.i(TAG, "[Android] $message")
}
