package br.com.leorvergani.escalaici.auth

data class CorporateIdentity(
    val tenantId: String,
    val objectId: String,
    val username: String,
    val displayName: String,
    val email: String?,
    val accountId: String
)
