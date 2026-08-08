package br.com.leorvergani.escalaici.kmp.lab.firebase.dto

import kotlinx.serialization.Serializable

/**
 * DTOs de Trocas (`trocasEscala`/`notificacoesTroca`) - contrato real do
 * Escala-ICI (`lib/trocasEscala.ts`, confirmado via `git show origin/main:...`
 * na FASE 16). `login`, nunca UID, identifica pessoas - mesma regra da FASE 15.
 */

@Serializable
enum class StatusTroca {
    PENDENTE_USUARIO,
    RECUSADA_USUARIO,
    CANCELADA_SOLICITANTE,
    PENDENTE_GESTOR,
    RECUSADA_GESTOR,
    APROVADA_PUBLICADA,
    EXPIRADA,
}

@Serializable
enum class AtorTroca { SOLICITANTE, DESTINATARIO, GESTOR, SISTEMA }

@Serializable
enum class TipoNotificacaoTroca {
    TROCA_SOLICITADA,
    TROCA_RECUSADA_USUARIO,
    TROCA_ACEITA_AGUARDANDO_GESTOR,
    TROCA_RECUSADA_GESTOR,
    TROCA_APROVADA_PUBLICADA,
    TROCA_CANCELADA,
}

@Serializable
data class EventoHistoricoTrocaDto(
    val tipo: String,
    val porLogin: String? = null,
    val porNome: String? = null,
    val porPerfil: AtorTroca,
    val em: String,
    val descricao: String,
)

@Serializable
data class SnapshotValidacaoTrocaDto(
    val solicitanteDocId: String,
    val destinatarioDocId: String,
    val turnoSolicitanteOriginal: String,
    val turnoDestinatarioOriginal: String,
)

@Serializable
data class SolicitacaoTrocaRealDto(
    val trocaId: String,
    val equipeId: String,
    val competencia: String,
    val solicitanteLogin: String,
    val solicitanteNome: String,
    val destinatarioLogin: String,
    val destinatarioNome: String,
    val data: String,
    val turnoSolicitanteAntes: String,
    val horarioSolicitanteAntes: String,
    val turnoDestinatarioAntes: String,
    val horarioDestinatarioAntes: String,
    val status: StatusTroca,
    val mensagemSolicitante: String? = null,
    val motivoRecusa: String? = null,
    val criadoEm: String,
    val atualizadoEm: String,
    val respondidoEm: String? = null,
    val aprovadoEm: String? = null,
    val publicadoEm: String? = null,
    val gestorLogin: String? = null,
    val gestorNome: String? = null,
    val historico: List<EventoHistoricoTrocaDto>,
    val snapshotValidacao: SnapshotValidacaoTrocaDto,
)

@Serializable
data class NotificacaoTrocaDto(
    val id: String,
    val destinatarioLogin: String,
    val equipeId: String,
    val tipo: TipoNotificacaoTroca,
    val titulo: String,
    val mensagem: String,
    val trocaId: String,
    val criadoPorLogin: String,
    val criadoEm: String,
    val lidaEm: String? = null,
    val acao: String = "ABRIR_TROCA",
)
