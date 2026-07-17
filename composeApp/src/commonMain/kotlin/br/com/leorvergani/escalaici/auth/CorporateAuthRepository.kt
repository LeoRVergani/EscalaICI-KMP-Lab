package br.com.leorvergani.escalaici.auth

import kotlinx.coroutines.flow.StateFlow

interface CorporateAuthRepository {
    val configurationState: CorporateAuthConfigurationState
    val state: StateFlow<CorporateAuthState>
    suspend fun restoreSession()
    suspend fun signInInteractive(host: CorporateAuthHost?)
    suspend fun signOut()
    fun enterDemoMode()
}
