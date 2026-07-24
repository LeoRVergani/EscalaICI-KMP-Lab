package br.com.leorvergani.escalaici.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
private data class MsalWebIdentityDto(
    val tenantId: String = "",
    val objectId: String? = null,
    val username: String = "",
    val displayName: String? = null,
    val email: String? = null,
    val accountId: String = ""
)

private val msalWebIdentityJson = Json { ignoreUnknownKeys = true }

fun parseIdentityJson(json: String): CorporateIdentity? =
    try {
        val dto = msalWebIdentityJson.decodeFromString(MsalWebIdentityDto.serializer(), json)
        CorporateIdentity(
            tenantId = dto.tenantId,
            objectId = dto.objectId?.takeIf { it.isNotBlank() } ?: dto.accountId,
            username = dto.username,
            displayName = dto.displayName?.takeIf { it.isNotBlank() } ?: dto.username,
            email = dto.email?.takeIf { it.isNotBlank() } ?: dto.username,
            accountId = dto.accountId,
        )
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: SerializationException) {
        null
    }

data class MsalWebRejection(
    val kind: String,
    val detail: String
) : Exception(detail)

fun MsalWebRejection.toCorporateAuthError(): CorporateAuthError =
    when (kind) {
        "popup_blocked" -> CorporateAuthError.Unknown(
            "Pop-up de login bloqueado pelo navegador. Permita pop-ups para este site e tente novamente."
        )
        "invalid_configuration" -> CorporateAuthError.InvalidConfiguration
        "cancelled", "already_in_progress" -> CorporateAuthError.Cancelled
        "account_not_found" -> CorporateAuthError.AccountNotFound
        "interaction_required" -> CorporateAuthError.InteractionRequired
        "tenant_not_allowed" -> CorporateAuthError.TenantNotAllowed
        "network_error" -> CorporateAuthError.NetworkError
        else -> CorporateAuthError.Unknown(detail = detail)
    }

/**
 * Decide o [CorporateAuthState] final de uma restauração de sessão Web (FASE 14J,
 * spec 67 seção 2.3), dada a identidade já obtida do cache local do MSAL.js
 * ([cachedIdentity], leitura pura sem rede) e o resultado best-effort de uma
 * tentativa de renovação silenciosa de token ([silentRefreshResult]).
 *
 * Sem conta em cache: sempre `SignedOut`. Com conta em cache: uma falha de rede
 * (ou de causa desconhecida) na renovação NUNCA derruba a sessão já restaurada -
 * só uma falha que prove que a sessão de fato não é mais válida
 * (`AccountNotFound`/`InteractionRequired`) derruba para `SignedOut`. Uma
 * renovação bem-sucedida atualiza a identidade para a versão mais recente.
 */
internal fun decideRestoredSessionState(
    cachedIdentity: CorporateIdentity?,
    silentRefreshResult: Result<CorporateIdentity?>
): CorporateAuthState {
    if (cachedIdentity == null) return CorporateAuthState.SignedOut

    silentRefreshResult.getOrNull()?.let { refreshed ->
        return CorporateAuthState.Authenticated(refreshed)
    }

    val rejection = silentRefreshResult.exceptionOrNull()
    val error = (rejection as? MsalWebRejection)?.toCorporateAuthError()
    return if (error == CorporateAuthError.AccountNotFound || error == CorporateAuthError.InteractionRequired) {
        CorporateAuthState.SignedOut
    } else {
        CorporateAuthState.Authenticated(cachedIdentity)
    }
}
