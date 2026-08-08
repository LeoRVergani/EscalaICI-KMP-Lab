package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.AtorTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.EventoHistoricoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SnapshotValidacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoNotificacaoTroca
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Converte documentos Firestore (`{ fields: {...} }`) de `trocasEscala`/
 * `notificacoesTroca` nos DTOs de Trocas, e o inverso para escrita
 * (`:commit`) - mesmo estilo/particionamento de `RemoteDtoMappers`.
 */
object TrocasRemoteMappers {
    private val codec = FirestoreValueCodec

    // --- leitura ---

    fun solicitacaoTroca(document: JsonObject): SolicitacaoTrocaRealDto? {
        val f = codec.fieldsOf(document)
        val status = codec.string(f, "status")?.let { runCatching { StatusTroca.valueOf(it) }.getOrNull() } ?: return null
        val snapshotFields = codec.map(f, "snapshotValidacao")
        return SolicitacaoTrocaRealDto(
            trocaId = codec.string(f, "trocaId") ?: return null,
            equipeId = codec.string(f, "equipeId") ?: return null,
            competencia = codec.string(f, "competencia") ?: return null,
            solicitanteLogin = codec.string(f, "solicitanteLogin") ?: return null,
            solicitanteNome = codec.string(f, "solicitanteNome") ?: "",
            destinatarioLogin = codec.string(f, "destinatarioLogin") ?: return null,
            destinatarioNome = codec.string(f, "destinatarioNome") ?: "",
            data = codec.string(f, "data") ?: return null,
            turnoSolicitanteAntes = codec.string(f, "turnoSolicitanteAntes") ?: "",
            horarioSolicitanteAntes = codec.string(f, "horarioSolicitanteAntes") ?: "",
            turnoDestinatarioAntes = codec.string(f, "turnoDestinatarioAntes") ?: "",
            horarioDestinatarioAntes = codec.string(f, "horarioDestinatarioAntes") ?: "",
            status = status,
            mensagemSolicitante = codec.string(f, "mensagemSolicitante"),
            motivoRecusa = codec.string(f, "motivoRecusa"),
            criadoEm = codec.string(f, "criadoEm") ?: "",
            atualizadoEm = codec.string(f, "atualizadoEm") ?: "",
            respondidoEm = codec.string(f, "respondidoEm"),
            aprovadoEm = codec.string(f, "aprovadoEm"),
            publicadoEm = codec.string(f, "publicadoEm"),
            gestorLogin = codec.string(f, "gestorLogin"),
            gestorNome = codec.string(f, "gestorNome"),
            historico = codec.arrayOfMaps(f, "historico").map(::eventoHistorico),
            snapshotValidacao = SnapshotValidacaoTrocaDto(
                solicitanteDocId = codec.string(snapshotFields, "solicitanteDocId") ?: "",
                destinatarioDocId = codec.string(snapshotFields, "destinatarioDocId") ?: "",
                turnoSolicitanteOriginal = codec.string(snapshotFields, "turnoSolicitanteOriginal") ?: "",
                turnoDestinatarioOriginal = codec.string(snapshotFields, "turnoDestinatarioOriginal") ?: "",
            ),
        )
    }

    fun notificacaoTroca(document: JsonObject): NotificacaoTrocaDto? {
        val f = codec.fieldsOf(document)
        val tipo = codec.string(f, "tipo")?.let { runCatching { TipoNotificacaoTroca.valueOf(it) }.getOrNull() } ?: return null
        return NotificacaoTrocaDto(
            id = codec.string(f, "id") ?: return null,
            destinatarioLogin = codec.string(f, "destinatarioLogin") ?: return null,
            equipeId = codec.string(f, "equipeId") ?: return null,
            tipo = tipo,
            titulo = codec.string(f, "titulo") ?: "",
            mensagem = codec.string(f, "mensagem") ?: "",
            trocaId = codec.string(f, "trocaId") ?: return null,
            criadoPorLogin = codec.string(f, "criadoPorLogin") ?: return null,
            criadoEm = codec.string(f, "criadoEm") ?: "",
            lidaEm = codec.string(f, "lidaEm"),
            acao = codec.string(f, "acao") ?: "ABRIR_TROCA",
        )
    }

    private fun eventoHistorico(fields: JsonObject): EventoHistoricoTrocaDto {
        val perfil = codec.string(fields, "porPerfil")?.let { runCatching { AtorTroca.valueOf(it) }.getOrNull() } ?: AtorTroca.SISTEMA
        return EventoHistoricoTrocaDto(
            tipo = codec.string(fields, "tipo") ?: "",
            porLogin = codec.string(fields, "porLogin"),
            porNome = codec.string(fields, "porNome"),
            porPerfil = perfil,
            em = codec.string(fields, "em") ?: "",
            descricao = codec.string(fields, "descricao") ?: "",
        )
    }

    // --- escrita ---

    /** Documento completo de uma troca nova - usado só em `create` (`FirestoreWrite.Create`). */
    fun encodeSolicitacaoTroca(troca: SolicitacaoTrocaRealDto): Map<String, JsonElement> = codec.buildFields {
        string("trocaId", troca.trocaId)
        string("equipeId", troca.equipeId)
        string("competencia", troca.competencia)
        string("solicitanteLogin", troca.solicitanteLogin)
        string("solicitanteNome", troca.solicitanteNome)
        string("destinatarioLogin", troca.destinatarioLogin)
        string("destinatarioNome", troca.destinatarioNome)
        string("data", troca.data)
        string("turnoSolicitanteAntes", troca.turnoSolicitanteAntes)
        string("horarioSolicitanteAntes", troca.horarioSolicitanteAntes)
        string("turnoDestinatarioAntes", troca.turnoDestinatarioAntes)
        string("horarioDestinatarioAntes", troca.horarioDestinatarioAntes)
        string("status", troca.status.name)
        stringOrNull("mensagemSolicitante", troca.mensagemSolicitante)
        stringOrNull("motivoRecusa", troca.motivoRecusa)
        string("criadoEm", troca.criadoEm)
        string("atualizadoEm", troca.atualizadoEm)
        stringOrNull("respondidoEm", troca.respondidoEm)
        stringOrNull("aprovadoEm", troca.aprovadoEm)
        stringOrNull("publicadoEm", troca.publicadoEm)
        stringOrNull("gestorLogin", troca.gestorLogin)
        stringOrNull("gestorNome", troca.gestorNome)
        arrayOfMaps("historico", troca.historico.map(::encodeEventoHistorico))
        map(
            "snapshotValidacao",
            codec.buildFields {
                string("solicitanteDocId", troca.snapshotValidacao.solicitanteDocId)
                string("destinatarioDocId", troca.snapshotValidacao.destinatarioDocId)
                string("turnoSolicitanteOriginal", troca.snapshotValidacao.turnoSolicitanteOriginal)
                string("turnoDestinatarioOriginal", troca.snapshotValidacao.turnoDestinatarioOriginal)
            },
        )
    }

    /** Documento completo de uma notificação nova - usado só em `create`. */
    fun encodeNotificacaoTroca(notificacao: NotificacaoTrocaDto): Map<String, JsonElement> = codec.buildFields {
        string("id", notificacao.id)
        string("destinatarioLogin", notificacao.destinatarioLogin)
        string("equipeId", notificacao.equipeId)
        string("tipo", notificacao.tipo.name)
        string("titulo", notificacao.titulo)
        string("mensagem", notificacao.mensagem)
        string("trocaId", notificacao.trocaId)
        string("criadoPorLogin", notificacao.criadoPorLogin)
        string("criadoEm", notificacao.criadoEm)
        stringOrNull("lidaEm", notificacao.lidaEm)
        string("acao", notificacao.acao)
    }

    /** Campos patch de uma troca (`status`+`atualizadoEm`+`historico`, e opcionalmente `respondidoEm`/`motivoRecusa`) - o `updateMask` de quem chama decide quais desses de fato são enviados. */
    fun encodeTrocaPatch(
        status: StatusTroca,
        atualizadoEm: String,
        historico: List<EventoHistoricoTrocaDto>,
        respondidoEm: String? = null,
        motivoRecusa: String? = null,
    ): Map<String, JsonElement> = codec.buildFields {
        string("status", status.name)
        string("atualizadoEm", atualizadoEm)
        arrayOfMaps("historico", historico.map(::encodeEventoHistorico))
        respondidoEm?.let { string("respondidoEm", it) }
        if (motivoRecusa != null || respondidoEm != null) {
            stringOrNull("motivoRecusa", motivoRecusa)
        }
    }

    /** Patch de "marcar como lida" - toca **somente** `lidaEm` (ajuste obrigatório da FASE 16, aprovação do usuário). */
    fun encodeNotificacaoLidaPatch(lidaEm: String): Map<String, JsonElement> = codec.buildFields {
        string("lidaEm", lidaEm)
    }

    private fun encodeEventoHistorico(evento: EventoHistoricoTrocaDto): Map<String, JsonElement> = codec.buildFields {
        string("tipo", evento.tipo)
        stringOrNull("porLogin", evento.porLogin)
        stringOrNull("porNome", evento.porNome)
        string("porPerfil", evento.porPerfil.name)
        string("em", evento.em)
        string("descricao", evento.descricao)
    }
}
