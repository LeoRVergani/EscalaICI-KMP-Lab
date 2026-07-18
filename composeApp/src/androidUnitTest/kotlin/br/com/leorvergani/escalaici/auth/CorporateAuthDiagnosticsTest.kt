package br.com.leorvergani.escalaici.auth

import com.microsoft.identity.client.exception.MsalArgumentException
import com.microsoft.identity.client.exception.MsalClientException
import kotlin.test.Test
import kotlin.test.assertEquals

class CorporateAuthDiagnosticsTest {
    @Test
    fun clientExceptionWithRedirectErrorMapsToRedirectSignatureMismatch() {
        val exception = MsalClientException("REDIRECT_URI_VALIDATION_ERROR", "validation failed")

        assertEquals(
            AuthDiagnosticCode.AUTH_REDIRECT_SIGNATURE_MISMATCH,
            exception.toDiagnosticCode(),
        )
    }

    @Test
    fun clientExceptionWithSignatureErrorMapsToRedirectSignatureMismatch() {
        val exception = MsalClientException("SIGNATURE_HASH_MISMATCH", "validation failed")

        assertEquals(
            AuthDiagnosticCode.AUTH_REDIRECT_SIGNATURE_MISMATCH,
            exception.toDiagnosticCode(),
        )
    }

    @Test
    fun clientExceptionWithOtherErrorMapsToMsalInitializationFailed() {
        val exception = MsalClientException("UNKNOWN_ERROR", "initialization failed")

        assertEquals(
            AuthDiagnosticCode.AUTH_MSAL_INITIALIZATION_FAILED,
            exception.toDiagnosticCode(),
        )
    }

    @Test
    fun argumentExceptionMapsToMsalResourceInvalid() {
        val exception = MsalArgumentException("operation", "argument", "invalid argument")

        assertEquals(
            AuthDiagnosticCode.AUTH_MSAL_RESOURCE_INVALID,
            exception.toDiagnosticCode(),
        )
    }
}
