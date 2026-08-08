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
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
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
 * Precondition falhou num `:commit` (409/412, ou 400 com status
 * FAILED_PRECONDITION/ALREADY_EXISTS/ABORTED no corpo - convencao real da
 * API do Firestore) - o documento mudou desde a leitura que originou esta
 * escrita. Quem chama deve reler o documento e informar o usuario, nunca
 * tentar reaplicar o `historico`/estado antigo por cima.
 */
class FirestoreConflictException(message: String) : Exception(message)

/**
 * Uma escrita dentro de um `:commit` em lote - sempre em `update`
 * (create-only via `currentDocument.exists=false`, ou patch parcial via
 * `updateMask`), espelhando exatamente os dois casos que `writeBatch` usa em
 * `lib/firebase/trocasRepository.ts` (nunca delete/runTransaction aqui).
 */
sealed interface FirestoreWrite {
    val collection: String
    val documentId: String
    val fields: Map<String, JsonElement>

    /** Cria um documento novo - falha com [FirestoreConflictException] se o id ja existir. */
    data class Create(
        override val collection: String,
        override val documentId: String,
        override val fields: Map<String, JsonElement>,
    ) : FirestoreWrite

    /** Atualiza campos especificos de um documento existente - falha com [FirestoreConflictException] se ele nao existir mais. */
    data class Patch(
        override val collection: String,
        override val documentId: String,
        override val fields: Map<String, JsonElement>,
        val updateMaskFieldPaths: List<String>,
    ) : FirestoreWrite
}

/**
 * Cliente REST puro (Ktor) para leitura/escrita no Firestore, autenticado
 * via ID token (`Authorization: Bearer`). Leitura (`getDocument`/`runQuery`)
 * e da FASE 15; escrita (`commit`) e nova na FASE 16, exclusiva de Trocas -
 * nunca grava diretamente em `turnosMes` como colaborador comum (Rules sao
 * quem decide isso, nao este cliente). Aponta para o host do Emulator
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

    /**
     * Grava um lote de escritas atomicamente (`:commit`), equivalente ao
     * `writeBatch(db)` do TS - todas as escritas sao aplicadas juntas ou
     * nenhuma e. Nunca usa `runTransaction` (reservado para a aprovacao do
     * gestor, fora do escopo desta fase no KMP).
     */
    suspend fun commit(idToken: String, writes: List<FirestoreWrite>): JsonObject {
        require(writes.isNotEmpty()) { "commit requer ao menos uma escrita." }
        val body = buildCommitBody(writes)
        val response: HttpResponse = try {
            httpClient.post("${config.firestoreBaseUrl}:commit") {
                header("Authorization", "Bearer $idToken")
                contentType(ContentType.Application.Json)
                setBody(body.toString())
            }
        } catch (t: Throwable) {
            throw FirestoreNetworkException("Falha de rede ao gravar.", t)
        }
        val rawBody = response.body<String>()
        return when {
            response.status == HttpStatusCode.Unauthorized -> throw FirestoreUnauthorizedException("Token expirado ao gravar.")
            response.status == HttpStatusCode.Forbidden -> throw FirestorePermissionDeniedException("Sem permissao para gravar.")
            isConflictResponse(response.status, rawBody) ->
                throw FirestoreConflictException("A solicitacao foi alterada por outra operacao - recarregue antes de tentar novamente.")
            !response.status.isSuccess() -> throw FirestoreNetworkException("Firestore respondeu ${response.status.value} para :commit.")
            else -> json.parseToJsonElement(rawBody).jsonObject
        }
    }

    private fun isConflictResponse(status: HttpStatusCode, rawBody: String): Boolean =
        status == HttpStatusCode.Conflict ||
            status == HttpStatusCode.PreconditionFailed ||
            (status == HttpStatusCode.BadRequest && CONFLICT_STATUS_MARKERS.any { rawBody.contains(it) })

    private fun buildCommitBody(writes: List<FirestoreWrite>): JsonObject = buildJsonObject {
        putJsonArray("writes") {
            writes.forEach { write ->
                addJsonObject {
                    putJsonObject("update") {
                        put("name", config.documentName(write.collection, write.documentId))
                        putJsonObject("fields") { write.fields.forEach { (name, value) -> put(name, value) } }
                    }
                    when (write) {
                        is FirestoreWrite.Create -> putJsonObject("currentDocument") { put("exists", false) }
                        is FirestoreWrite.Patch -> {
                            putJsonObject("updateMask") {
                                putJsonArray("fieldPaths") { write.updateMaskFieldPaths.forEach { add(it) } }
                            }
                            putJsonObject("currentDocument") { put("exists", true) }
                        }
                    }
                }
            }
        }
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

    companion object {
        /** Substrings do `status` retornado pela API no corpo de um 400 - a API do Firestore mapeia FAILED_PRECONDITION/ALREADY_EXISTS/ABORTED para HTTP 400, nao 409/412. */
        private val CONFLICT_STATUS_MARKERS = listOf("FAILED_PRECONDITION", "ALREADY_EXISTS", "ABORTED")
    }
}

private inline fun kotlinx.serialization.json.JsonArrayBuilder.addJsonObject(builder: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
    add(buildJsonObject(builder))
}
