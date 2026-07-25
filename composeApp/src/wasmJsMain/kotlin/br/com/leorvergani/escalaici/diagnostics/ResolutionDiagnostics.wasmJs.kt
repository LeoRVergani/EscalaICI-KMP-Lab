package br.com.leorvergani.escalaici.diagnostics

actual fun logResolutionFailure(diagnostic: ResolutionFailureDiagnostic) {
    val line = "[Web] step=${diagnostic.step} workspace=${diagnostic.workspaceId} " +
        "cause=${diagnostic.cause} exception=${diagnostic.exceptionType} " +
        "http=${diagnostic.httpStatus ?: "-"} msg=${diagnostic.sanitizedMessage}"
    consoleWarnResolutionFailure(line)
}

private fun consoleWarnResolutionFailure(line: String): Unit = js("console.warn(line)")
