package br.com.leorvergani.escalaici.auth

data class MsalWebConfig(
    val tenantId: String,
    val clientId: String,
    val redirectUri: String,
    val scopes: List<String>
) {
    val isConfigured: Boolean
        get() = tenantId.isNotBlank() && clientId.isNotBlank() && redirectUri.isNotBlank() &&
            !tenantId.startsWith("<") && !clientId.startsWith("<") && !redirectUri.startsWith("<")
}
