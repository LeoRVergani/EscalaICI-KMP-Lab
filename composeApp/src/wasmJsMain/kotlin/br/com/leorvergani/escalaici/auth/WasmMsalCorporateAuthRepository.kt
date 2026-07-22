package br.com.leorvergani.escalaici.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class WasmMsalCorporateAuthRepository(
    private val config: MsalWebConfig = platformMsalWebConfig
) : CorporateAuthRepository {
    override val configurationState: CorporateAuthConfigurationState =
        if (config.isConfigured) {
            CorporateAuthConfigurationState.CONFIGURED
        } else {
            CorporateAuthConfigurationState.NOT_CONFIGURED
        }

    private val mutableState = MutableStateFlow(
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) {
            CorporateAuthState.NotConfigured
        } else {
            CorporateAuthState.SignedOut
        }
    )

    override val state: StateFlow<CorporateAuthState> = mutableState

    override suspend fun restoreSession() {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

        runCatching {
            msalWebInit(config)
            msalWebGetActiveIdentityJson()
        }.getOrNull()
            ?: run {
                mutableState.value = CorporateAuthState.SignedOut
                return
            }

        val identity = runCatching {
            parseIdentityJson(msalWebAcquireTokenSilent(config.scopesCsv()))
        }.getOrNull()

        mutableState.value = identity?.let(CorporateAuthState::Authenticated) ?: CorporateAuthState.SignedOut
    }

    override suspend fun signInInteractive(host: CorporateAuthHost?) {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return
        if (mutableState.value == CorporateAuthState.Authenticating) return

        mutableState.value = CorporateAuthState.Authenticating
        val result = try {
            msalWebInit(config)
            val identity = parseIdentityJson(msalWebLoginPopup(config.scopesCsv()))
            identity?.let(CorporateAuthState::Authenticated)
                ?: CorporateAuthState.Failed(CorporateAuthError.AccountNotFound)
        } catch (rejection: MsalWebRejection) {
            CorporateAuthState.Failed(rejection.toCorporateAuthError())
        } catch (_: Throwable) {
            CorporateAuthState.Failed(CorporateAuthError.Unknown("Falha ao autenticar com a conta corporativa."))
        }
        mutableState.value = result
    }

    override suspend fun signOut() {
        if (configurationState == CorporateAuthConfigurationState.NOT_CONFIGURED) return

        runCatching { msalWebLogoutPopup() }
        mutableState.value = CorporateAuthState.SignedOut
    }

    override fun enterDemoMode() {
        mutableState.value = CorporateAuthState.Demo
    }
}

private fun MsalWebConfig.scopesCsv(): String = scopes.joinToString(",")
