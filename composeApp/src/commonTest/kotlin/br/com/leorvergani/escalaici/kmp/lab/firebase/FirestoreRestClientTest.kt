package br.com.leorvergani.escalaici.kmp.lab.firebase

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * `commit()` (escrita, FASE 16) - sem rede real, via `MockEngine`. Cobre o
 * ajuste obrigatório da aprovação do usuário: 401/403 continuam mapeados
 * como antes, e 409/412 (ou 400 com FAILED_PRECONDITION/ALREADY_EXISTS/
 * ABORTED no corpo - convenção real do Firestore) mapeiam para
 * [FirestoreConflictException], nunca sobrescrevendo silenciosamente.
 */
class FirestoreRestClientTest {

    private fun config() = EscalaIciFirebaseConfig(
        environment = FirebaseEnvironment.LOCAL_EMULATOR,
        projectId = "prod-unused",
        apiKey = "test-key",
        authDomain = "",
        appId = "",
        storageBucket = "",
        messagingSenderId = "",
        emulatorProjectId = "demo-test",
        emulatorAuthHost = "127.0.0.1",
        emulatorAuthPort = 9099,
        emulatorFirestoreHost = "127.0.0.1",
        emulatorFirestorePort = 8080,
    )

    private fun clientRespondingWith(status: HttpStatusCode, body: String = "{}"): FirestoreRestClient {
        val engine = MockEngine { _ ->
            respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        return FirestoreRestClient(HttpClient(engine), config())
    }

    private val umaEscrita = listOf(
        FirestoreWrite.Create("trocasEscala", "troca-1", mapOf("status" to FirestoreValueCodec.encodeString("PENDENTE_USUARIO"))),
    )

    @Test
    fun commit_unauthorized401_throwsFirestoreUnauthorizedException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.Unauthorized)
        assertFailsWith<FirestoreUnauthorizedException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_forbidden403_throwsFirestorePermissionDeniedException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.Forbidden)
        assertFailsWith<FirestorePermissionDeniedException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_conflict409_throwsFirestoreConflictException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.Conflict)
        assertFailsWith<FirestoreConflictException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_preconditionFailed412_throwsFirestoreConflictException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.PreconditionFailed)
        assertFailsWith<FirestoreConflictException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_badRequestWithFailedPrecondition_throwsFirestoreConflictException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.BadRequest, """{"error":{"status":"FAILED_PRECONDITION","message":"doc changed"}}""")
        assertFailsWith<FirestoreConflictException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_badRequestWithAlreadyExists_throwsFirestoreConflictException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.BadRequest, """{"error":{"status":"ALREADY_EXISTS"}}""")
        assertFailsWith<FirestoreConflictException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_badRequestWithoutPreconditionMarker_throwsFirestoreNetworkException() = runTest {
        val client = clientRespondingWith(HttpStatusCode.BadRequest, """{"error":{"status":"INVALID_ARGUMENT"}}""")
        assertFailsWith<FirestoreNetworkException> { client.commit("token", umaEscrita) }
    }

    @Test
    fun commit_success_returnsParsedBody() = runTest {
        val client = clientRespondingWith(HttpStatusCode.OK, """{"writeResults":[{"updateTime":"2026-08-20T10:00:00Z"}]}""")
        val result = client.commit("token", umaEscrita)
        assertTrue(result.containsKey("writeResults"))
    }

    @Test
    fun commit_requiresAtLeastOneWrite() = runTest {
        val client = clientRespondingWith(HttpStatusCode.OK)
        assertFailsWith<IllegalArgumentException> { client.commit("token", emptyList()) }
    }

    @Test
    fun commit_neverLogsOrLeaksToken_inRequestBody() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? TextContent)?.text
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val client = FirestoreRestClient(HttpClient(engine), config())
        client.commit("super-secret-token", umaEscrita)
        assertTrue(capturedBody?.contains("super-secret-token") != true)
    }

    @Test
    fun commit_createWrite_setsCurrentDocumentExistsFalse() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? TextContent)?.text
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val client = FirestoreRestClient(HttpClient(engine), config())
        client.commit(
            "token",
            listOf(FirestoreWrite.Create("trocasEscala", "troca-1", mapOf("status" to FirestoreValueCodec.encodeString("PENDENTE_USUARIO")))),
        )
        val body = requireNotNull(capturedBody)
        assertTrue(body.contains("\"exists\":false"))
        assertTrue(body.contains("trocasEscala/troca-1"))
    }

    @Test
    fun commit_patchWrite_setsUpdateMaskAndCurrentDocumentExistsTrue() = runTest {
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as? TextContent)?.text
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val client = FirestoreRestClient(HttpClient(engine), config())
        client.commit(
            "token",
            listOf(
                FirestoreWrite.Patch(
                    "trocasEscala",
                    "troca-1",
                    mapOf("lidaEm" to FirestoreValueCodec.encodeString("2026-08-20T10:00:00.000Z")),
                    listOf("lidaEm"),
                ),
            ),
        )
        val body = requireNotNull(capturedBody)
        assertTrue(body.contains("\"exists\":true"))
        assertTrue(body.contains("updateMask"))
        assertTrue(body.contains("lidaEm"))
    }

    @Test
    fun commit_postsToCommitSuffixOfFirestoreBaseUrl() = runTest {
        var capturedUrl: String? = null
        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            respond(content = "{}", status = HttpStatusCode.OK)
        }
        val client = FirestoreRestClient(HttpClient(engine), config())
        client.commit("token", umaEscrita)
        assertEquals(true, capturedUrl?.endsWith(":commit"))
    }
}
