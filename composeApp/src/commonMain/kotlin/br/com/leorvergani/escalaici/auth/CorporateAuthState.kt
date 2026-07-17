package br.com.leorvergani.escalaici.auth

sealed interface CorporateAuthState {
    data object NotConfigured : CorporateAuthState
    data object SignedOut : CorporateAuthState
    data object Authenticating : CorporateAuthState
    data class Authenticated(val identity: CorporateIdentity) : CorporateAuthState
    data class Failed(val error: CorporateAuthError) : CorporateAuthState
    data object Demo : CorporateAuthState
}
