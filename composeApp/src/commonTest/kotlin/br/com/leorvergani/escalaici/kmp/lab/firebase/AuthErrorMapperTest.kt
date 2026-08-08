package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlin.test.Test
import kotlin.test.assertEquals

/** Nunca uma mensagem generica para tudo (prompt FASE 15 secao 28) - cada codigo do Identity Toolkit cai num erro tipado distinto. */
class AuthErrorMapperTest {

    @Test
    fun mapsInvalidCredentialCodes_toInvalidCredentials() {
        assertEquals(EscalaIciError.INVALID_CREDENTIALS, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("EMAIL_NOT_FOUND", "")).error)
        assertEquals(EscalaIciError.INVALID_CREDENTIALS, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("INVALID_PASSWORD", "")).error)
        assertEquals(EscalaIciError.INVALID_CREDENTIALS, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("INVALID_LOGIN_CREDENTIALS", "")).error)
    }

    @Test
    fun mapsUserDisabled_toUserInactive() {
        assertEquals(EscalaIciError.USER_INACTIVE, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("USER_DISABLED", "")).error)
    }

    @Test
    fun mapsExpiredOrInvalidToken_toAuthRequired() {
        assertEquals(EscalaIciError.AUTH_REQUIRED, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("TOKEN_EXPIRED", "")).error)
        assertEquals(EscalaIciError.AUTH_REQUIRED, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("INVALID_REFRESH_TOKEN", "")).error)
    }

    @Test
    fun mapsUnknownCode_toUnknownError_ratherThanCrashing() {
        assertEquals(EscalaIciError.UNKNOWN_ERROR, AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException("SOMETHING_NEW", "")).error)
    }

    @Test
    fun everyMappedError_hasANonGenericMessage() {
        val messages = listOf("EMAIL_NOT_FOUND", "USER_DISABLED", "TOKEN_EXPIRED", "SOMETHING_NEW")
            .map { AuthErrorMapper.fromIdentityToolkit(IdentityToolkitException(it, "")).message }
        assertEquals(messages.toSet().size, messages.size)
    }
}
