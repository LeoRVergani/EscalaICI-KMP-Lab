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
