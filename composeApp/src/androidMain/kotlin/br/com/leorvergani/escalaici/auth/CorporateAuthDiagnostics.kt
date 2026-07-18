package br.com.leorvergani.escalaici.auth

import com.microsoft.identity.client.exception.MsalArgumentException
import com.microsoft.identity.client.exception.MsalClientException
import com.microsoft.identity.client.exception.MsalException

internal enum class AuthDiagnosticCode {
    AUTH_CONFIG_FILE_MISSING,
    AUTH_CONFIG_JSON_INVALID,
    AUTH_CONFIG_GENERATED_RESOURCE_MISSING,
    AUTH_CLIENT_ID_MISSING,
    AUTH_TENANT_ID_MISSING,
    AUTH_PACKAGE_MISMATCH,
    AUTH_DEBUG_SIGNATURE_MISMATCH,
    AUTH_RELEASE_SIGNATURE_MISMATCH,
    AUTH_REDIRECT_MISSING,
    AUTH_REDIRECT_INVALID,
    AUTH_REDIRECT_SIGNATURE_MISMATCH,
    AUTH_MANIFEST_PLACEHOLDER_UNRESOLVED,
    AUTH_AUTHORITY_INVALID,
    AUTH_ACCOUNT_MODE_MISMATCH,
    AUTH_MSAL_RESOURCE_INVALID,
    AUTH_MSAL_INITIALIZATION_FAILED,
}

internal fun MsalException.toDiagnosticCode(): AuthDiagnosticCode =
    when (this) {
        is MsalArgumentException -> AuthDiagnosticCode.AUTH_MSAL_RESOURCE_INVALID
        is MsalClientException ->
            if (
                errorCode.contains("REDIRECT", ignoreCase = true) ||
                errorCode.contains("SIGNATURE", ignoreCase = true)
            ) {
                AuthDiagnosticCode.AUTH_REDIRECT_SIGNATURE_MISMATCH
            } else {
                AuthDiagnosticCode.AUTH_MSAL_INITIALIZATION_FAILED
            }
        else -> AuthDiagnosticCode.AUTH_MSAL_INITIALIZATION_FAILED
    }
