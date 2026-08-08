package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/** Um filtro de igualdade simples (`where(field, '==', value)`), o unico tipo que a FASE 15 precisa - ver auditoria em FASE-15-FIREBASE-UNIFICADO.md. */
sealed interface FieldEquals {
    val field: String

    data class Text(override val field: String, val value: String) : FieldEquals
    data class Bool(override val field: String, val value: Boolean) : FieldEquals
}

class FirestorePermissionDeniedException(message: String) : Exception(message)
class FirestoreNetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Token ausente/expirado (401) - distinto de [FirestorePermissionDeniedException] (403, token valido mas sem acesso). O coordinator trata isto renovando o token e tentando novamente uma vez. */
class FirestoreUnauthorizedException(message: String) : Exception(message)

/**
 * Cliente REST puro (Ktor) para leitura no Firestore, autenticado via ID
 * token (`Authorization: Bearer`). So le - esta fase e read-only em relacao
 * a escala (prompt FASE 15 secao 29). Aponta para o host do Emulator
 * quando `config.usesEmulator`.
 */
class FirestoreRestClient(
    private val httpClient: HttpClient,
    private val config: EscalaIciFirebaseConfig,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun getDocument(idToken: String, collection: String, documentId: String): JsonObject? {
        val response: HttpResponse = try {
            httpClient.get("${config.firestoreBaseUrl}/$collection/$documentId") {
                header("Authorization", "Bearer $idToken")
            }
        } catch (t: Throwable) {
            throw FirestoreNetworkException("Falha de rede ao ler $collection/$documentId.", t)
        }
        return when {
            response.status == HttpStatusCode.NotFound -> null
            response.status == HttpStatusCode.Unauthorized -> throw FirestoreUnauthorizedException("Token expirado ao ler $collection/$documentId.")
            response.status == HttpStatusCode.Forbidden -> throw FirestorePermissionDeniedException("Sem permissao para ler $collection/$documentId.")
            !response.status.isSuccess() -> throw FirestoreNetworkException("Firestore respondeu ${response.status.value} para $collection/$documentId.")
            else -> json.parseToJsonElement(response.body<String>()).jsonObject
        }
    }

    suspend fun runQuery(
        idToken: String,
        collection: String,
        filters: List<FieldEquals>,
        limit: Int? = null,
    ): List<JsonObject> {
        val body = buildStructuredQuery(collection, filters, limit)
        val response: HttpResponse = try {
            httpClient.post("${config.firestoreBaseUrl}:runQuery") {
                header("Authorization", "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        } catch (t: Throwable) {
            throw FirestoreNetworkException("Falha de rede ao consultar $collection.", t)
        }
        if (response.status == HttpStatusCode.Unauthorized) {
            throw FirestoreUnauthorizedException("Token expirado ao consultar $collection.")
        }
        if (response.status == HttpStatusCode.Forbidden) {
            throw FirestorePermissionDeniedException("Sem permissao para consultar $collection.")
        }
        if (!response.status.isSuccess()) {
            throw FirestoreNetworkException("Firestore respondeu ${response.status.value} para :runQuery em $collection.")
        }
        val rows = json.parseToJsonElement(response.body<String>())
        val array = rows as? JsonArray ?: return emptyList()
        return array.mapNotNull { row -> row.jsonObject["document"]?.jsonObject }
    }

    private fun buildStructuredQuery(collection: String, filters: List<FieldEquals>, limit: Int?): JsonObject = buildJsonObject {
        putJsonObject("structuredQuery") {
            putJsonArray("from") { addJsonObject { put("collectionId", collection) } }
            if (filters.isNotEmpty()) {
                putJsonObject("where") {
                    if (filters.size == 1) {
                        putFieldFilter(filters.single())
                    } else {
                        putJsonObject("compositeFilter") {
                            put("op", "AND")
                            putJsonArray("filters") {
                                filters.forEach { filter -> addJsonObject { putFieldFilter(filter) } }
                            }
                        }
                    }
                }
            }
            limit?.let { put("limit", it) }
        }
    }

    private fun kotlinx.serialization.json.JsonObjectBuilder.putFieldFilter(filter: FieldEquals) {
        putJsonObject("fieldFilter") {
            putJsonObject("field") { put("fieldPath", filter.field) }
            put("op", "EQUAL")
            putJsonObject("value") {
                when (filter) {
                    is FieldEquals.Text -> put("stringValue", filter.value)
                    is FieldEquals.Bool -> put("booleanValue", filter.value)
                }
            }
        }
    }
}

private inline fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
    add(buildJsonObject(builder))
}
