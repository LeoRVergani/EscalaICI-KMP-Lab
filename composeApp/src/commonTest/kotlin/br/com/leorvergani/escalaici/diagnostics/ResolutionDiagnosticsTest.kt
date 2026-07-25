package br.com.leorvergani.escalaici.diagnostics

import br.com.leorvergani.escalaici.source.ScheduleSyncCause
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolutionDiagnosticsTest {
    @Test
    fun extractHttpStatusReadsParenthesizedCode() {
        assertEquals(404, extractHttpStatus("Firestore (404) em workspaces/ici-dev"))
    }

    @Test
    fun extractHttpStatusReadsBareCode() {
        assertEquals(403, extractHttpStatus("Forbidden 403 accessing document"))
    }

    @Test
    fun extractHttpStatusReturnsNullWhenAbsent() {
        assertNull(extractHttpStatus("Nao foi possivel conectar ao Firebase"))
        assertNull(extractHttpStatus(null))
    }

    @Test
    fun buildResolutionFailureDiagnosticNeverLeaksExceptionMessage() {
        val secretLookingMessage = "401 Unauthorized: Authorization Bearer eyJhbGciOiJIUzI1NiJ9.secret.token"
        val throwable = RuntimeException(secretLookingMessage)

        val diagnostic = buildResolutionFailureDiagnostic(
            step = "DemoPublicationResolver.loadOneAttempt",
            workspaceId = "ici-dev",
            throwable = throwable,
            sanitizedMessage = "Publicacao remota indisponivel."
        )

        assertEquals("ici-dev", diagnostic.workspaceId)
        assertEquals("DemoPublicationResolver.loadOneAttempt", diagnostic.step)
        assertEquals(ScheduleSyncCause.AUTH_REQUIRED, diagnostic.cause)
        assertEquals(401, diagnostic.httpStatus)
        assertEquals("RuntimeException", diagnostic.exceptionType)
        // sanitizedMessage vem de quem chama (safeMessage), nunca da excecao crua -
        // garante que nenhum token/segredo da mensagem original vaza para o log.
        assertEquals("Publicacao remota indisponivel.", diagnostic.sanitizedMessage)
    }
}
