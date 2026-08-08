package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Erro de autenticacao com o codigo bruto do Identity Toolkit (ex.: `EMAIL_NOT_FOUND`, `INVALID_PASSWORD`), mapeado por [AuthErrorMapper]. */
class IdentityToolkitException(val errorCode: String, message: String) : Exception(message)

@Serializable
private data class IdentityToolkitErrorBody(val error: IdentityToolkitErrorDetail? = null)

@Serializable
private data class IdentityToolkitErrorDetail(val message: String = "UNKNOWN_ERROR")

@Serializable
private data class SignInWithPasswordRequest(
    val email: String,
    val password: String,
    val returnSecureToken: Boolean = true,
)

@Serializable
data class IdentityToolkitTokenResponse(
    val idToken: String,
    val email: String,
    val refreshToken: String,
    val expiresIn: String,
    val localId: String,
)

@Serializable
private data class RefreshTokenResponse(
    @SerialName("id_token") val idToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: String,
    @SerialName("user_id") val userId: String,
)

/**
 * Cliente REST puro (Ktor) para o Identity Toolkit (login) e o Secure Token
 * Service (refresh) do Firebase Auth - equivalente autenticado do que o
 * antigo `FirestoreRestGateway.kt` fazia anonimamente. Aponta para os hosts
 * do Emulator quando `config.usesEmulator` (ver `EscalaIciFirebaseConfig`).
 * Nunca usa Admin SDK/service account - so a API publica de cliente,
 * exatamente como o `signInWithEmailAndPassword` do SDK JS do Escala-ICI.
 */
class IdentityToolkitAuthClient(
    private val httpClient: HttpClient,
    private val config: EscalaIciFirebaseConfig,
    // encodeDefaults=true e obrigatorio aqui: sem ele, kotlinx.serialization
    // omite `returnSecureToken` (fica igual ao valor padrao `true`) do corpo
    // codificado, e o Identity Toolkit real entao devolve um token sem
    // `refreshToken`/`expiresIn` - bug real encontrado testando contra
    // staging (o Firebase Emulator aceitava o corpo sem o campo sem
    // reclamar, por isso so apareceu contra a API real).
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true },
) {
    suspend fun signInWithPassword(email: String, password: String): IdentityToolkitTokenResponse {
        val response: HttpResponse = httpClient.post("${config.identityToolkitBaseUrl}/accounts:signInWithPassword") {
            contentType(ContentType.Application.Json)
            parameter("key", config.effectiveApiKey)
            setBody(json.encodeToString(SignInWithPasswordRequest(email, password)))
        }
        return decode(response)
    }

    suspend fun refreshIdToken(refreshToken: String): IdentityToolkitTokenResponse {
        val response: HttpResponse = httpClient.post("${config.secureTokenBaseUrl}/token") {
            contentType(ContentType.Application.FormUrlEncoded)
            parameter("key", config.effectiveApiKey)
            setBody("grant_type=refresh_token&refresh_token=$refreshToken")
        }
        if (!response.status.isSuccess()) throw errorFrom(response)
        val body: RefreshTokenResponse = json.decodeFromString(response.body())
        return IdentityToolkitTokenResponse(
            idToken = body.idToken,
            email = "",
            refreshToken = body.refreshToken,
            expiresIn = body.expiresIn,
            localId = body.userId,
        )
    }

    private suspend fun decode(response: HttpResponse): IdentityToolkitTokenResponse {
        if (!response.status.isSuccess()) throw errorFrom(response)
        return json.decodeFromString(response.body())
    }

    private suspend fun errorFrom(response: HttpResponse): IdentityToolkitException {
        val raw = runCatching { response.body<String>() }.getOrDefault("")
        val code = runCatching { json.decodeFromString<IdentityToolkitErrorBody>(raw).error?.message }
            .getOrNull() ?: "UNKNOWN_ERROR"
        return IdentityToolkitException(code, "Falha de autenticacao Firebase ($code).")
    }
}
