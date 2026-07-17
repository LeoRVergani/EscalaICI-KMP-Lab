package br.com.leorvergani.escalaici.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.yield

class FakeCorporateAuthRepository(
    initialConfigurationState: CorporateAuthConfigurationState = CorporateAuthConfigurationState.CONFIGURED,
    private val restoreResult: CorporateAuthState? = null,
    private val interactiveResult: CorporateAuthState = CorporateAuthState.Authenticated(
        identity = CorporateIdentity(
            tenantId = "fake-tenant-id",
            objectId = "fake-object-id",
            username = "usuario.corporativo@example.com",
            displayName = "Usuário Corporativo",
            email = "usuario.corporativo@example.com",
            accountId = "fake-account-id"
        )
    )
) : CorporateAuthRepository {
    override val configurationState: CorporateAuthConfigurationState = initialConfigurationState

    private val mutableState = MutableStateFlow(
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) {
            CorporateAuthState.NotConfigured
        } else {
            CorporateAuthState.SignedOut
        }
    )

    override val state: StateFlow<CorporateAuthState> = mutableState

    var signInInteractiveCallCount: Int = 0

    override suspend fun restoreSession() {
        if (configurationState == CorporateAuthConfigurationState.CONFIGURED && restoreResult != null) {
            mutableState.value = restoreResult
        }
    }

    override suspend fun signInInteractive(host: CorporateAuthHost?) {
        signInInteractiveCallCount++
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) {
            mutableState.value = CorporateAuthState.NotConfigured
            return
        }

        mutableState.value = CorporateAuthState.Authenticating
        yield() // permite a quem observa `state` coletar o valor transiente antes do resultado final
        mutableState.value = interactiveResult
    }

    override suspend fun signOut() {
        if (mutableState.value != CorporateAuthState.NotConfigured) {
            mutableState.value = CorporateAuthState.SignedOut
        }
    }

    override fun enterDemoMode() {
        mutableState.value = CorporateAuthState.Demo
    }
}
