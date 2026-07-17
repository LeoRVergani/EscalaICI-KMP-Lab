package br.com.leorvergani.escalaici.auth

import androidx.compose.runtime.Composable

/** Cada plataforma resolve o host de autenticação (Android: Activity atual; Web: sempre nulo). */
@Composable
expect fun rememberCorporateAuthHost(): CorporateAuthHost?
