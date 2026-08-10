package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.AtorTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.EventoHistoricoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SnapshotValidacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoNotificacaoTroca
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `calcularTrocasBadge`/`notificacoesNaoLidasDaTroca` (FASE 16, hardening
 * item 3) - sem I/O. Cobre exatamente os casos exigidos na auditoria
 * pré-staging: uma troca deduplicada com sua notificação nunca conta 2.
 */
class TrocasBadgeTest {

    private fun troca(trocaId: String, destinatarioLogin: String, status: StatusTroca = StatusTroca.PENDENTE_USUARIO) = SolicitacaoTrocaRealDto(
        trocaId = trocaId,
        equipeId = "EQ_SOC",
        competencia = "2026-08",
        solicitanteLogin = "ana.silva",
        solicitanteNome = "Ana Silva",
        destinatarioLogin = destinatarioLogin,
        destinatarioNome = "Carlos Souza",
        data = "2026-08-20",
        turnoSolicitanteAntes = "T",
        horarioSolicitanteAntes = "13:00–19:00",
        turnoDestinatarioAntes = "M",
        horarioDestinatarioAntes = "07:00–13:00",
        status = status,
        mensagemSolicitante = null,
        motivoRecusa = null,
        criadoEm = "2026-08-18T10:00:00.000Z",
        atualizadoEm = "2026-08-18T10:00:00.000Z",
        respondidoEm = null,
        aprovadoEm = null,
        publicadoEm = null,
        gestorLogin = null,
        gestorNome = null,
        historico = listOf(
            EventoHistoricoTrocaDto(tipo = "SOLICITACAO_CRIADA", porLogin = "ana.silva", porNome = "Ana Silva", porPerfil = AtorTroca.SOLICITANTE, em = "2026-08-18T10:00:00.000Z", descricao = "Solicitação criada"),
        ),
        snapshotValidacao = SnapshotValidacaoTrocaDto("EQ_SOC_ana.silva_2026-08", "EQ_SOC_carlos.souza_2026-08", "T", "M"),
    )

    private fun notificacao(id: String, destinatarioLogin: String, trocaId: String, lidaEm: String? = null, tipo: TipoNotificacaoTroca = TipoNotificacaoTroca.TROCA_SOLICITADA) = NotificacaoTrocaDto(
        id = id,
        destinatarioLogin = destinatarioLogin,
        equipeId = "EQ_SOC",
        tipo = tipo,
        titulo = "Nova solicitação de troca",
        mensagem = "Ana Silva quer trocar o turno do dia 2026-08-20 com você.",
        trocaId = trocaId,
        criadoPorLogin = "ana.silva",
        criadoEm = "2026-08-18T10:00:00.000Z",
        lidaEm = lidaEm,
    )

    @Test
    fun umaTrocaMaisNotificacaoCorrespondente_conta1NaoDois() {
        val badge = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", destinatarioLogin = "carlos.souza")),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC")),
        )
        assertEquals(1, badge.total)
    }

    @Test
    fun duasTrocasMaisNotificacoesCorrespondentes_conta2() {
        val badge = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", "carlos.souza"), troca("DEF", "carlos.souza")),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC"), notificacao("n2", "carlos.souza", "DEF")),
        )
        assertEquals(2, badge.total)
    }

    @Test
    fun trocaSemNotificacaoCorrespondente_conta1() {
        val badge = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", "carlos.souza")),
            notificacoes = emptyList(),
        )
        assertEquals(1, badge.total)
        assertEquals(1, badge.paraResponder)
        assertEquals(0, badge.notificacoesDistintas)
    }

    @Test
    fun notificacaoDistintaSemTrocaAcionavel_contaComoItemDistinto() {
        // "Sua troca foi aceita, aguardando o gestor" - a troca ja avancou pra PENDENTE_GESTOR,
        // entao nao e mais "acionavel" para quem solicitou, mas a notificacao ainda e algo nao visto.
        val badge = calcularTrocasBadge(
            loginAtual = "ana.silva",
            trocas = listOf(troca("ABC", destinatarioLogin = "carlos.souza", status = StatusTroca.PENDENTE_GESTOR)),
            notificacoes = listOf(notificacao("n1", "ana.silva", "ABC", tipo = TipoNotificacaoTroca.TROCA_ACEITA_AGUARDANDO_GESTOR)),
        )
        assertEquals(1, badge.total)
        assertEquals(0, badge.paraResponder)
        assertEquals(1, badge.notificacoesDistintas)
    }

    @Test
    fun notificacaoLida_naoAumentaBadge() {
        val badge = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = emptyList(),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC", lidaEm = "2026-08-19T08:00:00.000Z")),
        )
        assertEquals(0, badge.total)
    }

    @Test
    fun abrirTrocaEMarcarNotificacaoComoLida_mantemBadgeConsistente() {
        // Antes: troca ainda PENDENTE_USUARIO (usuario so abriu, nao respondeu) + notificacao nao lida.
        val antes = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", "carlos.souza")),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC")),
        )
        assertEquals(1, antes.total)

        // Depois de marcar a notificacao como lida (troca ainda PENDENTE_USUARIO - usuario ainda nao respondeu):
        // o badge continua 1, refletindo a acao pendente (responder), nao a notificacao.
        val depoisDeMarcarLida = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", "carlos.souza")),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC", lidaEm = "2026-08-19T08:00:00.000Z")),
        )
        assertEquals(1, depoisDeMarcarLida.total)

        // So depois de responder de verdade (troca deixa de ser PENDENTE_USUARIO) o badge cai a 0.
        val depoisDeResponder = calcularTrocasBadge(
            loginAtual = "carlos.souza",
            trocas = listOf(troca("ABC", "carlos.souza", status = StatusTroca.PENDENTE_GESTOR)),
            notificacoes = listOf(notificacao("n1", "carlos.souza", "ABC", lidaEm = "2026-08-19T08:00:00.000Z")),
        )
        assertEquals(0, depoisDeResponder.total)
    }

    @Test
    fun semTrocasNemNotificacoes_badgeZero() {
        val badge = calcularTrocasBadge("carlos.souza", emptyList(), emptyList())
        assertEquals(0, badge.total)
    }

    // --- notificacoesNaoLidasDaTroca ---

    @Test
    fun notificacoesNaoLidasDaTroca_filtraPorTrocaIdENaoLida() {
        val notificacoes = listOf(
            notificacao("n1", "carlos.souza", "ABC"),
            notificacao("n2", "carlos.souza", "ABC", lidaEm = "2026-08-19T08:00:00.000Z"),
            notificacao("n3", "carlos.souza", "DEF"),
        )
        val pendentes = notificacoesNaoLidasDaTroca(notificacoes, "ABC")
        assertEquals(listOf("n1"), pendentes.map { it.id })
    }

    @Test
    fun notificacoesNaoLidasDaTroca_semCorrespondencia_listaVazia() {
        val pendentes = notificacoesNaoLidasDaTroca(listOf(notificacao("n1", "carlos.souza", "ABC")), "OUTRA")
        assertTrue(pendentes.isEmpty())
    }
}
