package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.AtorTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.EventoHistoricoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SnapshotValidacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoNotificacaoTroca
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Payload de escrita (`encodeX`) lido de volta pelo decoder (`solicitacaoTroca`/`notificacaoTroca`) - garante que o que o app envia ao Firestore é exatamente o que ele reconhece ao ler de novo. */
class TrocasRemoteMappersTest {

    private fun asDocument(fields: Map<String, kotlinx.serialization.json.JsonElement>): JsonObject = buildJsonObject {
        putJsonObject("fields") { fields.forEach { (k, v) -> put(k, v) } }
    }

    private val trocaOriginal = SolicitacaoTrocaRealDto(
        trocaId = "troca-1",
        equipeId = "EQ_SOC",
        competencia = "2026-08",
        solicitanteLogin = "ana.silva",
        solicitanteNome = "Ana Silva",
        destinatarioLogin = "carlos.souza",
        destinatarioNome = "Carlos Souza",
        data = "2026-08-20",
        turnoSolicitanteAntes = "T",
        horarioSolicitanteAntes = "13:00–19:00",
        turnoDestinatarioAntes = "M",
        horarioDestinatarioAntes = "07:00–13:00",
        status = StatusTroca.PENDENTE_USUARIO,
        mensagemSolicitante = "Pode trocar comigo?",
        motivoRecusa = null,
        criadoEm = "2026-08-18T10:00:00.000Z",
        atualizadoEm = "2026-08-18T10:00:00.000Z",
        respondidoEm = null,
        aprovadoEm = null,
        publicadoEm = null,
        gestorLogin = null,
        gestorNome = null,
        historico = listOf(
            EventoHistoricoTrocaDto(
                tipo = "SOLICITACAO_CRIADA",
                porLogin = "ana.silva",
                porNome = "Ana Silva",
                porPerfil = AtorTroca.SOLICITANTE,
                em = "2026-08-18T10:00:00.000Z",
                descricao = "Solicitação criada",
            ),
        ),
        snapshotValidacao = SnapshotValidacaoTrocaDto(
            solicitanteDocId = "EQ_SOC_ana.silva_2026-08",
            destinatarioDocId = "EQ_SOC_carlos.souza_2026-08",
            turnoSolicitanteOriginal = "T",
            turnoDestinatarioOriginal = "M",
        ),
    )

    @Test
    fun solicitacaoTroca_roundTripsAllScalarFields() {
        val document = asDocument(TrocasRemoteMappers.encodeSolicitacaoTroca(trocaOriginal))
        val decoded = TrocasRemoteMappers.solicitacaoTroca(document)

        assertEquals(trocaOriginal.trocaId, decoded?.trocaId)
        assertEquals(trocaOriginal.solicitanteLogin, decoded?.solicitanteLogin)
        assertEquals(trocaOriginal.destinatarioLogin, decoded?.destinatarioLogin)
        assertEquals(trocaOriginal.status, decoded?.status)
        assertEquals(trocaOriginal.mensagemSolicitante, decoded?.mensagemSolicitante)
        assertEquals(trocaOriginal.data, decoded?.data)
    }

    @Test
    fun solicitacaoTroca_roundTripsNullableFieldsAsNull() {
        val document = asDocument(TrocasRemoteMappers.encodeSolicitacaoTroca(trocaOriginal))
        val decoded = TrocasRemoteMappers.solicitacaoTroca(document)
        assertNull(decoded?.motivoRecusa)
        assertNull(decoded?.respondidoEm)
        assertNull(decoded?.gestorLogin)
    }

    @Test
    fun solicitacaoTroca_roundTripsHistoricoArray() {
        val document = asDocument(TrocasRemoteMappers.encodeSolicitacaoTroca(trocaOriginal))
        val decoded = TrocasRemoteMappers.solicitacaoTroca(document)
        assertEquals(1, decoded?.historico?.size)
        assertEquals("SOLICITACAO_CRIADA", decoded?.historico?.first()?.tipo)
        assertEquals(AtorTroca.SOLICITANTE, decoded?.historico?.first()?.porPerfil)
    }

    @Test
    fun solicitacaoTroca_roundTripsSnapshotValidacao() {
        val document = asDocument(TrocasRemoteMappers.encodeSolicitacaoTroca(trocaOriginal))
        val decoded = TrocasRemoteMappers.solicitacaoTroca(document)
        assertEquals(trocaOriginal.snapshotValidacao, decoded?.snapshotValidacao)
    }

    @Test
    fun trocaPatch_setsOnlyStatusAtualizadoEmAndHistorico() {
        val patch = TrocasRemoteMappers.encodeTrocaPatch(
            status = StatusTroca.CANCELADA_SOLICITANTE,
            atualizadoEm = "2026-08-19T09:00:00.000Z",
            historico = trocaOriginal.historico,
        )
        assertEquals(setOf("status", "atualizadoEm", "historico"), patch.keys)
    }

    @Test
    fun notificacaoTroca_roundTrips() {
        val notificacao = NotificacaoTrocaDto(
            id = "notif-1",
            destinatarioLogin = "carlos.souza",
            equipeId = "EQ_SOC",
            tipo = TipoNotificacaoTroca.TROCA_SOLICITADA,
            titulo = "Nova solicitação de troca",
            mensagem = "Ana Silva quer trocar o turno do dia 2026-08-20 com você.",
            trocaId = "troca-1",
            criadoPorLogin = "ana.silva",
            criadoEm = "2026-08-18T10:00:00.000Z",
            lidaEm = null,
        )
        val document = asDocument(TrocasRemoteMappers.encodeNotificacaoTroca(notificacao))
        val decoded = TrocasRemoteMappers.notificacaoTroca(document)
        assertEquals(notificacao.id, decoded?.id)
        assertEquals(notificacao.tipo, decoded?.tipo)
        assertNull(decoded?.lidaEm)
    }

    @Test
    fun notificacaoLidaPatch_setsOnlyLidaEm() {
        val patch = TrocasRemoteMappers.encodeNotificacaoLidaPatch("2026-08-20T12:00:00.000Z")
        assertEquals(setOf("lidaEm"), patch.keys)
    }
}
