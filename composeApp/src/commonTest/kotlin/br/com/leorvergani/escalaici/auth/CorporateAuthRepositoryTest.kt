package br.com.leorvergani.escalaici.auth

import br.com.leorvergani.escalaici.repository.InMemoryAuthSessionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val sampleIdentity = CorporateIdentity(
    tenantId = "d2d23346-e737-4cac-96ec-fb25e7889f01",
    objectId = "00000000-0000-0000-0000-000000000001",
    username = "ana.paula@ici.tec.br",
    displayName = "Ana Paula",
    email = "ana.paula@ici.tec.br",
    accountId = "msal-account-1"
)

class CorporateAuthRepositoryTest {

    // 1. configuração ausente
    @Test
    fun notConfigured_startsInNotConfiguredState() = runTest {
        val repository = FakeCorporateAuthRepository(
            initialConfigurationState = CorporateAuthConfigurationState.NOT_CONFIGURED
        )
        assertEquals(CorporateAuthConfigurationState.NOT_CONFIGURED, repository.configurationState)
        assertEquals(CorporateAuthState.NotConfigured, repository.state.value)
    }

    // 2. estado desconectado
    @Test
    fun configured_startsSignedOut() = runTest {
        val repository = FakeCorporateAuthRepository()
        assertEquals(CorporateAuthConfigurationState.CONFIGURED, repository.configurationState)
        assertEquals(CorporateAuthState.SignedOut, repository.state.value)
    }

    // 3. login iniciado (estado transiente Authenticating observável)
    @Test
    fun signInInteractive_passesThroughAuthenticatingBeforeFinalResult() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Authenticated(sampleIdentity)
        )
        val observed = mutableListOf<CorporateAuthState>()
        val collector = launch { repository.state.collect { observed += it } }

        repository.signInInteractive(host = null)

        assertTrue(observed.contains(CorporateAuthState.Authenticating), "esperava observar o estado Authenticating em trânsito, coletado: $observed")
        collector.cancel()
    }

    // 4. login concluído
    @Test
    fun signInInteractive_endsAuthenticatedWithIdentity() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Authenticated(sampleIdentity)
        )
        repository.signInInteractive(host = null)

        val result = assertIs<CorporateAuthState.Authenticated>(repository.state.value)
        assertEquals(sampleIdentity, result.identity)
        assertEquals(1, repository.signInInteractiveCallCount)
    }

    // 5. login cancelado
    @Test
    fun signInInteractive_canReportCancelled() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Failed(CorporateAuthError.Cancelled)
        )
        repository.signInInteractive(host = null)

        val failed = assertIs<CorporateAuthState.Failed>(repository.state.value)
        assertEquals(CorporateAuthError.Cancelled, failed.error)
    }

    // 6. erro de rede
    @Test
    fun signInInteractive_canReportNetworkError() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Failed(CorporateAuthError.NetworkError)
        )
        repository.signInInteractive(host = null)

        val failed = assertIs<CorporateAuthState.Failed>(repository.state.value)
        assertEquals(CorporateAuthError.NetworkError, failed.error)
        assertEquals("Sem conexão para autenticar; tente novamente quando a rede estiver disponível.", failed.error.defaultMessage())
    }

    // 7. tenant incorreto
    @Test
    fun signInInteractive_canReportTenantNotAllowed() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Failed(CorporateAuthError.TenantNotAllowed)
        )
        repository.signInInteractive(host = null)

        val failed = assertIs<CorporateAuthState.Failed>(repository.state.value)
        assertEquals(CorporateAuthError.TenantNotAllowed, failed.error)
    }

    // 8. conta ausente
    @Test
    fun signInInteractive_canReportAccountNotFound() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Failed(CorporateAuthError.AccountNotFound)
        )
        repository.signInInteractive(host = null)

        val failed = assertIs<CorporateAuthState.Failed>(repository.state.value)
        assertEquals(CorporateAuthError.AccountNotFound, failed.error)
    }

    // 9. interação necessária
    @Test
    fun restoreSession_canReportInteractionRequired() = runTest {
        val repository = FakeCorporateAuthRepository(
            restoreResult = CorporateAuthState.Failed(CorporateAuthError.InteractionRequired)
        )
        repository.restoreSession()

        val failed = assertIs<CorporateAuthState.Failed>(repository.state.value)
        assertEquals(CorporateAuthError.InteractionRequired, failed.error)
    }

    // 10. restauração silenciosa (sucesso)
    @Test
    fun restoreSession_canRestoreAuthenticatedSilently() = runTest {
        val repository = FakeCorporateAuthRepository(
            restoreResult = CorporateAuthState.Authenticated(sampleIdentity)
        )
        assertEquals(CorporateAuthState.SignedOut, repository.state.value, "antes de restaurar, deve estar desconectado")

        repository.restoreSession()

        val result = assertIs<CorporateAuthState.Authenticated>(repository.state.value)
        assertEquals(sampleIdentity, result.identity)
        assertEquals(0, repository.signInInteractiveCallCount, "restauração silenciosa não deve contar como login interativo")
    }

    // restoreSession sem cenário de script não altera o estado (continua desconectado)
    @Test
    fun restoreSession_withoutScriptedResult_staysSignedOut() = runTest {
        val repository = FakeCorporateAuthRepository()
        repository.restoreSession()
        assertEquals(CorporateAuthState.SignedOut, repository.state.value)
    }

    // 11. logout
    @Test
    fun signOut_returnsToSignedOutFromAuthenticated() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Authenticated(sampleIdentity)
        )
        repository.signInInteractive(host = null)
        assertIs<CorporateAuthState.Authenticated>(repository.state.value)

        repository.signOut()

        assertEquals(CorporateAuthState.SignedOut, repository.state.value)
    }

    @Test
    fun signOut_neverRevivesNotConfigured() = runTest {
        val repository = FakeCorporateAuthRepository(
            initialConfigurationState = CorporateAuthConfigurationState.NOT_CONFIGURED
        )
        repository.signOut()
        assertEquals(CorporateAuthState.NotConfigured, repository.state.value)
    }

    // 12. identidade preserva tenantId e objectId
    @Test
    fun identity_preservesTenantIdAndObjectId() = runTest {
        val repository = FakeCorporateAuthRepository(
            interactiveResult = CorporateAuthState.Authenticated(sampleIdentity)
        )
        repository.signInInteractive(host = null)

        val identity = assertIs<CorporateAuthState.Authenticated>(repository.state.value).identity
        assertEquals(sampleIdentity.tenantId, identity.tenantId)
        assertEquals(sampleIdentity.objectId, identity.objectId)
    }

    // 13. token não aparece nos modelos visíveis
    @Test
    fun identity_hasNoTokenLikeField() {
        val fieldNames = listOf(
            CorporateIdentity::tenantId.name,
            CorporateIdentity::objectId.name,
            CorporateIdentity::username.name,
            CorporateIdentity::displayName.name,
            CorporateIdentity::email.name,
            CorporateIdentity::accountId.name
        )
        val suspiciousNames = fieldNames.filter { name ->
            val lower = name.lowercase()
            "token" in lower || "secret" in lower || "password" in lower
        }
        assertTrue(suspiciousNames.isEmpty(), "CorporateIdentity não deveria ter nenhum campo de token/senha, achou: $suspiciousNames")
    }

    // 14. modo demonstração é separado da autenticação real
    @Test
    fun demoMode_isDistinctFromAuthenticated() = runTest {
        val repository = FakeCorporateAuthRepository()
        repository.enterDemoMode()

        assertEquals(CorporateAuthState.Demo, repository.state.value)
        val demoAsState: CorporateAuthState = CorporateAuthState.Demo
        assertNotEquals(demoAsState, CorporateAuthState.Authenticated(sampleIdentity))
        assertFalse(repository.state.value is CorporateAuthState.Authenticated)
        assertEquals(0, repository.signInInteractiveCallCount, "entrar em modo demonstração não deve disparar login interativo")
    }

    // 15. login mock não é tratado como conta corporativa
    @Test
    fun mockMemberLogin_neverAffectsCorporateAuthState() = runTest {
        val corporateAuth = FakeCorporateAuthRepository()
        val mockSession = InMemoryAuthSessionRepository()

        mockSession.signIn(memberId = "lvergani@ici.tec.br")

        assertEquals("lvergani@ici.tec.br", mockSession.currentMemberId())
        assertEquals(CorporateAuthState.SignedOut, corporateAuth.state.value, "login de teste (mock) não deve autenticar o CorporateAuthRepository")
        assertFalse(corporateAuth.state.value is CorporateAuthState.Authenticated)
    }

    // 16. Web continua em estado não configurado (contrato comum; o stub Wasm real é validado no adapter da FASE 14b-1 Tarefa 3)
    @Test
    fun notConfiguredRepository_behavesLikeWebStub_regardlessOfCalls() = runTest {
        val repository = FakeCorporateAuthRepository(
            initialConfigurationState = CorporateAuthConfigurationState.NOT_CONFIGURED
        )
        repository.restoreSession()
        repository.signInInteractive(host = null)

        assertEquals(CorporateAuthState.NotConfigured, repository.state.value)
    }

    // 17. ausência de configuração não causa crash
    @Test
    fun notConfigured_allOperationsCompleteWithoutThrowing() = runTest {
        val repository = FakeCorporateAuthRepository(
            initialConfigurationState = CorporateAuthConfigurationState.NOT_CONFIGURED
        )
        repository.restoreSession()
        repository.signInInteractive(host = null)
        repository.signOut()
        repository.enterDemoMode()
        // enterDemoMode ainda troca o estado para Demo mesmo sem configuração — isso é intencional
        // (o modo demonstração nunca depende de MSAL estar configurado).
        assertEquals(CorporateAuthState.Demo, repository.state.value)
    }

    @Test
    fun defaultMessage_isDistinctForEveryErrorVariant() {
        val messages = listOf(
            CorporateAuthError.Cancelled,
            CorporateAuthError.TenantNotAllowed,
            CorporateAuthError.AccountNotFound,
            CorporateAuthError.InteractionRequired,
            CorporateAuthError.NetworkError,
            CorporateAuthError.InvalidConfiguration,
            CorporateAuthError.Unknown(detail = null)
        ).map { it.defaultMessage() }

        assertEquals(messages.size, messages.toSet().size, "cada erro deve ter uma mensagem distinta, sem duplicatas")
    }

    @Test
    fun defaultMessage_unknownUsesDetailWhenPresent() {
        val withDetail = CorporateAuthError.Unknown(detail = "MsalDeclinedScopeException: escopo recusado")
        assertEquals("MsalDeclinedScopeException: escopo recusado", withDetail.defaultMessage())

        val withoutDetail = CorporateAuthError.Unknown(detail = null)
        assertNull((withoutDetail).detail)
        assertTrue(withoutDetail.defaultMessage().isNotBlank())
    }
}
