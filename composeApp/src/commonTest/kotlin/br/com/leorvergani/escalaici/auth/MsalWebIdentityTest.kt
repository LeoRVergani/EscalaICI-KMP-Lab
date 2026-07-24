package br.com.leorvergani.escalaici.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MsalWebIdentityTest {
    @Test
    fun parseIdentityJson_completeJsonMapsIdentity() {
        val identity = parseIdentityJson(
            """
            {
              "tenantId": "tenant-1",
              "objectId": "object-1",
              "username": "ana.paula@ici.tec.br",
              "displayName": "Ana Paula",
              "email": "ana.paula@ici.tec.br",
              "accountId": "account-1"
            }
            """.trimIndent()
        )

        assertEquals(
            CorporateIdentity(
                tenantId = "tenant-1",
                objectId = "object-1",
                username = "ana.paula@ici.tec.br",
                displayName = "Ana Paula",
                email = "ana.paula@ici.tec.br",
                accountId = "account-1",
            ),
            identity,
        )
    }

    @Test
    fun parseIdentityJson_missingOptionalFieldsUsesFallbacks() {
        val identity = parseIdentityJson(
            """
            {
              "tenantId": "tenant-1",
              "username": "ana.paula@ici.tec.br",
              "accountId": "account-1"
            }
            """.trimIndent()
        )

        assertEquals(
            CorporateIdentity(
                tenantId = "tenant-1",
                objectId = "account-1",
                username = "ana.paula@ici.tec.br",
                displayName = "ana.paula@ici.tec.br",
                email = "ana.paula@ici.tec.br",
                accountId = "account-1",
            ),
            identity,
        )
    }

    @Test
    fun parseIdentityJson_malformedJsonReturnsNull() {
        assertNull(parseIdentityJson("{"))
    }

    @Test
    fun rejectionMapsAllKnownKinds() {
        assertIs<CorporateAuthError.Unknown>(MsalWebRejection("popup_blocked", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.InvalidConfiguration, MsalWebRejection("invalid_configuration", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.Cancelled, MsalWebRejection("cancelled", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.Cancelled, MsalWebRejection("already_in_progress", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.AccountNotFound, MsalWebRejection("account_not_found", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.InteractionRequired, MsalWebRejection("interaction_required", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.TenantNotAllowed, MsalWebRejection("tenant_not_allowed", "raw").toCorporateAuthError())
        assertEquals(CorporateAuthError.NetworkError, MsalWebRejection("network_error", "raw").toCorporateAuthError())
    }

    @Test
    fun rejectionUnknownUsesSanitizedDetail() {
        assertEquals(
            CorporateAuthError.Unknown("mensagem sanitizada"),
            MsalWebRejection("unknown", "mensagem sanitizada").toCorporateAuthError(),
        )
    }

    private val sampleIdentity = CorporateIdentity(
        tenantId = "tenant-1",
        objectId = "object-1",
        username = "ana.paula@ici.tec.br",
        displayName = "Ana Paula",
        email = "ana.paula@ici.tec.br",
        accountId = "account-1",
    )

    @Test
    fun decideRestoredSessionState_noCachedIdentityIsAlwaysSignedOut() {
        assertEquals(
            CorporateAuthState.SignedOut,
            decideRestoredSessionState(cachedIdentity = null, silentRefreshResult = Result.success(sampleIdentity)),
        )
        assertEquals(
            CorporateAuthState.SignedOut,
            decideRestoredSessionState(
                cachedIdentity = null,
                silentRefreshResult = Result.failure(MsalWebRejection("network_error", "sem rede")),
            ),
        )
    }

    @Test
    fun decideRestoredSessionState_networkFailureDuringRefreshPreservesCachedSession() {
        val result = decideRestoredSessionState(
            cachedIdentity = sampleIdentity,
            silentRefreshResult = Result.failure(MsalWebRejection("network_error", "sem rede")),
        )
        assertEquals(CorporateAuthState.Authenticated(sampleIdentity), result)
    }

    @Test
    fun decideRestoredSessionState_unknownFailureDuringRefreshPreservesCachedSession() {
        val result = decideRestoredSessionState(
            cachedIdentity = sampleIdentity,
            silentRefreshResult = Result.failure(RuntimeException("erro inesperado")),
        )
        assertEquals(CorporateAuthState.Authenticated(sampleIdentity), result)
    }

    @Test
    fun decideRestoredSessionState_interactionRequiredEndsSession() {
        val result = decideRestoredSessionState(
            cachedIdentity = sampleIdentity,
            silentRefreshResult = Result.failure(MsalWebRejection("interaction_required", "precisa logar de novo")),
        )
        assertEquals(CorporateAuthState.SignedOut, result)
    }

    @Test
    fun decideRestoredSessionState_accountNotFoundEndsSession() {
        val result = decideRestoredSessionState(
            cachedIdentity = sampleIdentity,
            silentRefreshResult = Result.failure(MsalWebRejection("account_not_found", "conta removida")),
        )
        assertEquals(CorporateAuthState.SignedOut, result)
    }

    @Test
    fun decideRestoredSessionState_successfulRefreshUsesRenewedIdentity() {
        val renewed = sampleIdentity.copy(displayName = "Ana Paula Renovada")
        val result = decideRestoredSessionState(
            cachedIdentity = sampleIdentity,
            silentRefreshResult = Result.success(renewed),
        )
        assertEquals(CorporateAuthState.Authenticated(renewed), result)
    }

    @Test
    fun msalWebConfigIsConfiguredOnlyForRealValues() {
        assertEquals(
            true,
            MsalWebConfig(
                tenantId = "tenant-1",
                clientId = "client-1",
                redirectUri = "http://localhost:8080/",
                scopes = listOf("openid", "profile", "User.Read"),
            ).isConfigured,
        )
        assertEquals(
            false,
            MsalWebConfig(
                tenantId = "",
                clientId = "client-1",
                redirectUri = "http://localhost:8080/",
                scopes = listOf("User.Read"),
            ).isConfigured,
        )
        assertEquals(
            false,
            MsalWebConfig(
                tenantId = "<TENANT_ID>",
                clientId = "client-1",
                redirectUri = "http://localhost:8080/",
                scopes = listOf("User.Read"),
            ).isConfigured,
        )
    }
}
