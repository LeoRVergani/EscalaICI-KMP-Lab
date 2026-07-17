package br.com.leorvergani.escalaici.auth

sealed interface CorporateAuthError {
    data object Cancelled : CorporateAuthError
    data object TenantNotAllowed : CorporateAuthError
    data object AccountNotFound : CorporateAuthError
    data object InteractionRequired : CorporateAuthError
    data object NetworkError : CorporateAuthError
    data object InvalidConfiguration : CorporateAuthError
    data class Unknown(val detail: String? = null) : CorporateAuthError
}

fun CorporateAuthError.defaultMessage(): String =
    when (this) {
        CorporateAuthError.Cancelled -> "Login corporativo cancelado pelo usuário."
        CorporateAuthError.TenantNotAllowed -> "Esta conta não pertence à organização autorizada."
        CorporateAuthError.AccountNotFound -> "Nenhuma conta corporativa foi encontrada neste dispositivo."
        CorporateAuthError.InteractionRequired -> "É necessário entrar novamente com a conta corporativa."
        CorporateAuthError.NetworkError -> "Sem conexão para autenticar; tente novamente quando a rede estiver disponível."
        CorporateAuthError.InvalidConfiguration -> "A configuração de autenticação corporativa está inválida neste ambiente."
        is CorporateAuthError.Unknown -> detail ?: "Não foi possível concluir a autenticação corporativa."
    }
