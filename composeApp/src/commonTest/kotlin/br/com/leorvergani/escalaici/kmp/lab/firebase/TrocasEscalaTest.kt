package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Porte puro de `lib/trocasEscala.ts` - sem I/O, mesma cobertura do TS (FASE 16). */
class TrocasEscalaTest {

    // --- rotulos/severidade ---

    @Test
    fun rotuloStatusTroca_coversAllSevenStatuses() {
        for (status in StatusTroca.entries) {
            assertTrue(ROTULO_STATUS_TROCA.containsKey(status), "faltou rotulo para $status")
            assertTrue(SEVERIDADE_STATUS_TROCA.containsKey(status), "faltou severidade para $status")
        }
    }

    @Test
    fun rotuloStatusTroca_matchesExactLabelsFromSpec() {
        assertEquals("Aguardando colega", ROTULO_STATUS_TROCA[StatusTroca.PENDENTE_USUARIO])
        assertEquals("Recusada pelo colega", ROTULO_STATUS_TROCA[StatusTroca.RECUSADA_USUARIO])
        assertEquals("Cancelada", ROTULO_STATUS_TROCA[StatusTroca.CANCELADA_SOLICITANTE])
        assertEquals("Aguardando gestor", ROTULO_STATUS_TROCA[StatusTroca.PENDENTE_GESTOR])
        assertEquals("Recusada pelo gestor", ROTULO_STATUS_TROCA[StatusTroca.RECUSADA_GESTOR])
        assertEquals("Concluída", ROTULO_STATUS_TROCA[StatusTroca.APROVADA_PUBLICADA])
        assertEquals("Expirada", ROTULO_STATUS_TROCA[StatusTroca.EXPIRADA])
    }

    @Test
    fun severidade_successOnlyForApprovedPublished() {
        assertEquals(SeveridadeStatusTroca.SUCCESS, SEVERIDADE_STATUS_TROCA[StatusTroca.APROVADA_PUBLICADA])
        assertEquals(SeveridadeStatusTroca.DANGER, SEVERIDADE_STATUS_TROCA[StatusTroca.RECUSADA_USUARIO])
        assertEquals(SeveridadeStatusTroca.DANGER, SEVERIDADE_STATUS_TROCA[StatusTroca.RECUSADA_GESTOR])
        assertEquals(SeveridadeStatusTroca.WARNING, SEVERIDADE_STATUS_TROCA[StatusTroca.PENDENTE_USUARIO])
        assertEquals(SeveridadeStatusTroca.WARNING, SEVERIDADE_STATUS_TROCA[StatusTroca.PENDENTE_GESTOR])
        assertEquals(SeveridadeStatusTroca.NEUTRAL, SEVERIDADE_STATUS_TROCA[StatusTroca.CANCELADA_SOLICITANTE])
        assertEquals(SeveridadeStatusTroca.NEUTRAL, SEVERIDADE_STATUS_TROCA[StatusTroca.EXPIRADA])
    }

    // --- status ativo / filtros de aba ---

    @Test
    fun statusEhAtivo_trueOnlyForPendenteUsuarioAndPendenteGestor() {
        assertTrue(statusEhAtivo(StatusTroca.PENDENTE_USUARIO))
        assertTrue(statusEhAtivo(StatusTroca.PENDENTE_GESTOR))
        for (status in StatusTroca.entries - setOf(StatusTroca.PENDENTE_USUARIO, StatusTroca.PENDENTE_GESTOR)) {
            assertFalse(statusEhAtivo(status), "$status nao deveria ser ativo")
        }
    }

    // --- transicoes ---

    @Test
    fun transicaoPermitida_pendenteUsuario_podeIrParaAsTresSaidasValidas() {
        assertTrue(transicaoPermitida(StatusTroca.PENDENTE_USUARIO, StatusTroca.RECUSADA_USUARIO))
        assertTrue(transicaoPermitida(StatusTroca.PENDENTE_USUARIO, StatusTroca.PENDENTE_GESTOR))
        assertTrue(transicaoPermitida(StatusTroca.PENDENTE_USUARIO, StatusTroca.CANCELADA_SOLICITANTE))
    }

    @Test
    fun transicaoPermitida_pendenteUsuario_naoPodeIrDiretoParaStatusDeGestor() {
        assertFalse(transicaoPermitida(StatusTroca.PENDENTE_USUARIO, StatusTroca.APROVADA_PUBLICADA))
        assertFalse(transicaoPermitida(StatusTroca.PENDENTE_USUARIO, StatusTroca.RECUSADA_GESTOR))
    }

    @Test
    fun transicaoPermitida_pendenteGestor_podeIrParaAprovadaOuRecusada() {
        assertTrue(transicaoPermitida(StatusTroca.PENDENTE_GESTOR, StatusTroca.APROVADA_PUBLICADA))
        assertTrue(transicaoPermitida(StatusTroca.PENDENTE_GESTOR, StatusTroca.RECUSADA_GESTOR))
    }

    @Test
    fun transicaoPermitida_pendenteGestor_naoVoltaParaPendenteUsuario() {
        assertFalse(transicaoPermitida(StatusTroca.PENDENTE_GESTOR, StatusTroca.PENDENTE_USUARIO))
    }

    @Test
    fun transicaoPermitida_statusTerminal_naoTemSaida() {
        val terminais = listOf(StatusTroca.RECUSADA_USUARIO, StatusTroca.CANCELADA_SOLICITANTE, StatusTroca.RECUSADA_GESTOR, StatusTroca.APROVADA_PUBLICADA, StatusTroca.EXPIRADA)
        for (de in terminais) {
            for (para in StatusTroca.entries) {
                assertFalse(transicaoPermitida(de, para), "$de nao deveria poder ir para $para")
            }
        }
    }

    // --- limite de mensagem ---

    @Test
    fun limiteMensagemTroca_e280() {
        assertEquals(280, LIMITE_MENSAGEM_TROCA)
    }

    // --- validarNovaSolicitacaoTroca ---

    private val diaValido = DiaRemoteDto(c = "M")

    @Test
    fun validarNovaSolicitacao_semErros_quandoTudoValido() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = DiaRemoteDto(c = "M"),
                diaDestinatario = DiaRemoteDto(c = "T"),
            ),
        )
        assertTrue(erros.isEmpty())
    }

    @Test
    fun validarNovaSolicitacao_rejeitaTrocarConsigoMesmo() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "ana.silva",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = diaValido,
                diaDestinatario = diaValido,
            ),
        )
        assertTrue(erros.any { it.contains("outro colaborador") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaDestinatarioEmBranco() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = diaValido,
                diaDestinatario = null,
            ),
        )
        assertTrue(erros.any { it.contains("Informe o colaborador") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaSolicitanteInativo() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = false,
                destinatarioAtivo = true,
                diaSolicitante = diaValido,
                diaDestinatario = diaValido,
            ),
        )
        assertTrue(erros.any { it.contains("solicitante precisa estar ativo") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaDestinatarioInativo() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = true,
                destinatarioAtivo = false,
                diaSolicitante = diaValido,
                diaDestinatario = diaValido,
            ),
        )
        assertTrue(erros.any { it.contains("destinatário precisa estar ativo") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaQuandoSolicitanteNaoTemTurnoNesseDia() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = null,
                diaDestinatario = diaValido,
            ),
        )
        assertTrue(erros.any { it.contains("Você não tem turno") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaQuandoDestinatarioNaoTemTurnoNesseDia() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = diaValido,
                diaDestinatario = null,
            ),
        )
        assertTrue(erros.any { it.contains("colega não tem turno") })
    }

    @Test
    fun validarNovaSolicitacao_rejeitaQuandoJaEstaoNoMesmoTurno() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "carlos.souza",
                solicitanteAtivo = true,
                destinatarioAtivo = true,
                diaSolicitante = DiaRemoteDto(c = "M"),
                diaDestinatario = DiaRemoteDto(c = "M"),
            ),
        )
        assertTrue(erros.any { it.contains("mesmo turno") })
    }

    @Test
    fun validarNovaSolicitacao_acumulaMultiplosErros() {
        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = "ana.silva",
                destinatarioLogin = "ana.silva",
                solicitanteAtivo = false,
                destinatarioAtivo = false,
                diaSolicitante = null,
                diaDestinatario = null,
            ),
        )
        // "mesmo login" (1) + solicitante inativo (2) + destinatario inativo (3) + sem turno solicitante (4) + sem turno destinatario (5).
        assertEquals(5, erros.size)
    }
}
