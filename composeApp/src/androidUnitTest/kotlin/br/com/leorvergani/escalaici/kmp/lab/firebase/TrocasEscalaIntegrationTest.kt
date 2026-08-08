package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Testes de integracao REAIS de Trocas (FASE 16) - exercitam
 * `FirestoreRestClient.commit()`/`TrocasEscalaRepository` contra as
 * Firestore Rules reais do Emulator, sem nenhum mock (prompt FASE 16 seção
 * 29 - "Não mockar FirestoreRestClient"). Só rodam com
 * `ESCALAICI_FIREBASE_EMULATOR=true` e os dados semeados por
 * `scripts/seed-firebase-emulator.sh` (estendido nesta fase com
 * carlos.souza, mesma equipe de ana.silva, e mariana.rocha, equipe
 * diferente):
 *
 *   ESCALAICI_FIREBASE_EMULATOR=true ./gradlew :composeApp:testDebugUnitTest --tests "*TrocasEscalaIntegrationTest*"
 *
 * Cada cenário usa uma data diferente (hoje/+1/+2/+3) para não colidir com
 * a checagem de solicitação duplicada entre testes.
 */
class TrocasEscalaIntegrationTest {

    private val equipeId = "EQ_TESTE"
    private val competencia = "2026-08"
    private val hoje = LocalDate.now().toString()
    private val hoje1 = LocalDate.now().plusDays(1).toString()
    private val hoje2 = LocalDate.now().plusDays(2).toString()
    private val hoje3 = LocalDate.now().plusDays(3).toString()

    private fun config() = EscalaIciFirebaseConfig(
        environment = FirebaseEnvironment.LOCAL_EMULATOR,
        projectId = "", apiKey = "", authDomain = "", appId = "",
        storageBucket = "", messagingSenderId = "",
        emulatorProjectId = "demo-escalaici-kmp",
        emulatorAuthHost = "127.0.0.1", emulatorAuthPort = 9099,
        emulatorFirestoreHost = "127.0.0.1", emulatorFirestorePort = 8080,
    )

    private fun requireEmulator() = assumeTrue(
        "Defina ESCALAICI_FIREBASE_EMULATOR=true (com o Firebase Emulator rodando e semeado, incluindo carlos.souza/mariana.rocha) para rodar este teste.",
        System.getenv("ESCALAICI_FIREBASE_EMULATOR") == "true",
    )

    private class Sessao(val idToken: String, val login: String, val nome: String)

    private suspend fun login(client: FirestoreRestClient, authClient: IdentityToolkitAuthClient, email: String, nome: String): Sessao {
        val response = try {
            authClient.signInWithPassword(email, "TesteEmulator123!")
        } catch (e: IdentityToolkitException) {
            fail("Login falhou para $email (${e.errorCode}) - usuario de teste nao existe no Auth Emulator ou senha incorreta. Rode scripts/seed-firebase-emulator.sh dentro do emulador.")
        }
        return Sessao(response.idToken, loginFromEmail(email), nome)
    }

    @Test
    fun anaSolicitaTroca_carlosRecebeEAceita_vaiParaPendenteGestor() = runBlocking {
        requireEmulator()
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config())
        val firestore = FirestoreRestClient(httpClient, config())
        val repository = TrocasEscalaRepository(firestore)

        val ana = login(firestore, authClient, "ana.silva@empresa.com", "Ana Silva")
        val carlos = login(firestore, authClient, "carlos.souza@empresa.com", "Carlos Souza")

        val trocaId = repository.criarSolicitacao(
            ana.idToken,
            EntradaCriarSolicitacaoTroca(
                equipeId = equipeId,
                competencia = competencia,
                data = hoje,
                solicitante = ParticipanteTroca(ana.login, ana.nome, ativo = true),
                destinatario = ParticipanteTroca(carlos.login, carlos.nome, ativo = true),
                mensagem = "Pode trocar comigo hoje?",
                catalogo = mapOf(),
            ),
        )

        // Carlos recebe - aparece nas trocas dele como destinatario pendente.
        val trocasDoCarlos = repository.buscarMinhasTrocas(carlos.idToken, equipeId, competencia, carlos.login)
        val recebida = trocasDoCarlos.firstOrNull { it.trocaId == trocaId }
        assertNotNull(recebida, "Carlos deveria ver a troca recebida de Ana")
        assertEquals(StatusTroca.PENDENTE_USUARIO, recebida.status)
        assertEquals("ana.silva", recebida.solicitanteLogin)

        // Carlos aceita.
        repository.responder(carlos.idToken, trocaId, ParticipanteTroca(carlos.login, carlos.nome, ativo = true), aceitar = true)

        val trocaAtualizada = repository.buscarMinhasTrocas(ana.idToken, equipeId, competencia, ana.login).first { it.trocaId == trocaId }
        assertEquals(StatusTroca.PENDENTE_GESTOR, trocaAtualizada.status)
        assertNotNull(trocaAtualizada.respondidoEm)

        // Ana foi notificada do aceite.
        val notificacoesAna = repository.buscarNotificacoes(ana.idToken, ana.login)
        val notificacaoAceite = notificacoesAna.firstOrNull { it.trocaId == trocaId }
        assertNotNull(notificacaoAceite, "Ana deveria receber notificação do aceite")
        assertNull(notificacaoAceite.lidaEm)

        // marcarNotificacaoComoLida toca so lidaEm.
        repository.marcarNotificacaoComoLida(ana.idToken, notificacaoAceite.id)
        val notificacoesAtualizadas = repository.buscarNotificacoes(ana.idToken, ana.login)
        val notificacaoLida = notificacoesAtualizadas.first { it.id == notificacaoAceite.id }
        assertNotNull(notificacaoLida.lidaEm)
        assertEquals(notificacaoAceite.titulo, notificacaoLida.titulo)
        assertEquals(notificacaoAceite.criadoEm, notificacaoLida.criadoEm)
    }

    @Test
    fun anaSolicitaTroca_carlosRecusa_vaiParaRecusadaUsuario() = runBlocking {
        requireEmulator()
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config())
        val firestore = FirestoreRestClient(httpClient, config())
        val repository = TrocasEscalaRepository(firestore)

        val ana = login(firestore, authClient, "ana.silva@empresa.com", "Ana Silva")
        val carlos = login(firestore, authClient, "carlos.souza@empresa.com", "Carlos Souza")

        val trocaId = repository.criarSolicitacao(
            ana.idToken,
            EntradaCriarSolicitacaoTroca(
                equipeId = equipeId, competencia = competencia, data = hoje1,
                solicitante = ParticipanteTroca(ana.login, ana.nome, ativo = true),
                destinatario = ParticipanteTroca(carlos.login, carlos.nome, ativo = true),
                mensagem = "", catalogo = mapOf(),
            ),
        )

        repository.responder(carlos.idToken, trocaId, ParticipanteTroca(carlos.login, carlos.nome, ativo = true), aceitar = false, motivoRecusa = "Já tenho compromisso")

        val trocaAtualizada = repository.buscarMinhasTrocas(ana.idToken, equipeId, competencia, ana.login).first { it.trocaId == trocaId }
        assertEquals(StatusTroca.RECUSADA_USUARIO, trocaAtualizada.status)
        assertEquals("Já tenho compromisso", trocaAtualizada.motivoRecusa)
    }

    @Test
    fun anaSolicitaTroca_anaCancela_vaiParaCanceladaSolicitante() = runBlocking {
        requireEmulator()
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config())
        val firestore = FirestoreRestClient(httpClient, config())
        val repository = TrocasEscalaRepository(firestore)

        val ana = login(firestore, authClient, "ana.silva@empresa.com", "Ana Silva")
        val carlos = login(firestore, authClient, "carlos.souza@empresa.com", "Carlos Souza")

        val trocaId = repository.criarSolicitacao(
            ana.idToken,
            EntradaCriarSolicitacaoTroca(
                equipeId = equipeId, competencia = competencia, data = hoje2,
                solicitante = ParticipanteTroca(ana.login, ana.nome, ativo = true),
                destinatario = ParticipanteTroca(carlos.login, carlos.nome, ativo = true),
                mensagem = "", catalogo = mapOf(),
            ),
        )

        repository.cancelar(ana.idToken, trocaId, ParticipanteTroca(ana.login, ana.nome, ativo = true))

        val trocaAtualizada = repository.buscarMinhasTrocas(ana.idToken, equipeId, competencia, ana.login).first { it.trocaId == trocaId }
        assertEquals(StatusTroca.CANCELADA_SOLICITANTE, trocaAtualizada.status)
    }

    @Test
    fun carlosNaoPodeCancelarSolicitacaoDeAna_permissionDenied() = runBlocking {
        requireEmulator()
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config())
        val firestore = FirestoreRestClient(httpClient, config())
        val repository = TrocasEscalaRepository(firestore)

        val ana = login(firestore, authClient, "ana.silva@empresa.com", "Ana Silva")
        val carlos = login(firestore, authClient, "carlos.souza@empresa.com", "Carlos Souza")

        val trocaId = repository.criarSolicitacao(
            ana.idToken,
            EntradaCriarSolicitacaoTroca(
                equipeId = equipeId, competencia = competencia, data = hoje3,
                solicitante = ParticipanteTroca(ana.login, ana.nome, ativo = true),
                destinatario = ParticipanteTroca(carlos.login, carlos.nome, ativo = true),
                mensagem = "", catalogo = mapOf(),
            ),
        )

        // Verificacao client-side (mensagem amigavel) - a rule tambem nao permitiria essa escrita.
        assertFailsWith<EscalaIciException> {
            repository.cancelar(carlos.idToken, trocaId, ParticipanteTroca(carlos.login, carlos.nome, ativo = true))
        }
        Unit
    }

    @Test
    fun usuarioDeOutraEquipe_naoListaTrocasDeEquipeAlheia() = runBlocking {
        requireEmulator()
        val httpClient = HttpClient(CIO)
        val authClient = IdentityToolkitAuthClient(httpClient, config())
        val firestore = FirestoreRestClient(httpClient, config())
        val repository = TrocasEscalaRepository(firestore)

        val mariana = login(firestore, authClient, "mariana.rocha@empresa.com", "Mariana Rocha")

        // mariana.rocha (EQ_OUTRA) tentando consultar trocas de EQ_TESTE - Rules negam por equipe (podeOperarNaEquipe()).
        val exception = assertFailsWith<EscalaIciException> {
            repository.buscarMinhasTrocas(mariana.idToken, equipeId, competencia, mariana.login)
        }
        assertTrue(
            exception.error == EscalaIciError.PERMISSION_DENIED,
            "esperava PERMISSION_DENIED, recebeu ${exception.error}: ${exception.message}",
        )
    }
}
