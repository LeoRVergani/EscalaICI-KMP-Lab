package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca

/** Contagem discreta para o badge da aba Trocas (FASE 16, seção 21) - solicitações recebidas pendentes + notificações não lidas. */
data class TrocasBadge(val paraResponder: Int, val naoLidas: Int) {
    val total: Int get() = paraResponder + naoLidas
}

/**
 * Fachada de Trocas para a UI - resolve login/equipeId/competencia a partir
 * do [LoggedScheduleSyncCoordinator] (a mesma sessao de Hoje/Escala/Perfil,
 * nunca um segundo login) e cuida do token/retry-on-401
 * ([LoggedScheduleSyncCoordinator.withFreshToken]) antes de chamar
 * [TrocasEscalaRepository]/[TeamScheduleRepository].
 */
class TrocasSession(
    private val syncCoordinator: LoggedScheduleSyncCoordinator,
    private val trocasRepository: TrocasEscalaRepository,
    val teamScheduleRepository: TeamScheduleRepository,
) {
    suspend fun minhasTrocas(): List<SolicitacaoTrocaRealDto> {
        val identidade = identidadeOuFalhar()
        return comToken { token -> trocasRepository.buscarMinhasTrocas(token, identidade.equipeId, identidade.competencia, identidade.login) }
    }

    suspend fun notificacoes(): List<NotificacaoTrocaDto> {
        val login = identidadeOuFalhar().login
        return comToken { token -> trocasRepository.buscarNotificacoes(token, login) }
    }

    /** Usuarios ativos + `turnosMes` publicadas + catalogo da equipe/competencia atual - cache compartilhado, ver [TeamScheduleRepository]. */
    suspend fun teamSnapshot(forceRefresh: Boolean = false): TeamScheduleSnapshot {
        val identidade = identidadeOuFalhar()
        return comToken { token -> teamScheduleRepository.snapshot(token, identidade.equipeId, identidade.competencia, forceRefresh) }
    }

    suspend fun criarSolicitacao(
        snapshot: TeamScheduleSnapshot,
        data: String,
        destinatarioLogin: String,
        destinatarioNome: String,
        destinatarioAtivo: Boolean,
        mensagem: String,
    ): String {
        val identidade = identidadeOuFalhar()
        val entrada = EntradaCriarSolicitacaoTroca(
            equipeId = identidade.equipeId,
            competencia = identidade.competencia,
            data = data,
            solicitante = ParticipanteTroca(identidade.login, identidade.nome, identidade.ativo),
            destinatario = ParticipanteTroca(destinatarioLogin, destinatarioNome, destinatarioAtivo),
            mensagem = mensagem,
            catalogo = snapshot.catalogo,
        )
        return comToken { token -> trocasRepository.criarSolicitacao(token, entrada) }
    }

    suspend fun cancelar(trocaId: String) {
        val identidade = identidadeOuFalhar()
        comToken { token -> trocasRepository.cancelar(token, trocaId, ParticipanteTroca(identidade.login, identidade.nome, identidade.ativo)) }
    }

    suspend fun responder(trocaId: String, aceitar: Boolean, motivoRecusa: String? = null) {
        val identidade = identidadeOuFalhar()
        comToken { token -> trocasRepository.responder(token, trocaId, ParticipanteTroca(identidade.login, identidade.nome, identidade.ativo), aceitar, motivoRecusa) }
    }

    suspend fun marcarNotificacaoComoLida(notificacaoId: String) {
        comToken { token -> trocasRepository.marcarNotificacaoComoLida(token, notificacaoId) }
    }

    /** Limpa o cache de equipe/escala compartilhado - chamado pelo refresh manual do cabecalho (FASE 16). */
    suspend fun invalidateTeamSnapshot() {
        teamScheduleRepository.invalidate()
    }

    /**
     * Contagem para o badge discreto da aba/ícone Trocas - atualizada ao
     * abrir Trocas, após ação do usuário, no refresh manual e ao voltar ao
     * foreground (spec FASE 16 seção 21). Nunca chamada em polling/timer.
     */
    suspend fun badge(): TrocasBadge {
        val identidade = identidadeOuFalhar()
        val trocas = comToken { token -> trocasRepository.buscarMinhasTrocas(token, identidade.equipeId, identidade.competencia, identidade.login) }
        val notificacoes = comToken { token -> trocasRepository.buscarNotificacoes(token, identidade.login) }
        val paraResponder = trocas.count { it.destinatarioLogin == identidade.login && it.status == StatusTroca.PENDENTE_USUARIO }
        val naoLidas = notificacoes.count { it.lidaEm == null }
        return TrocasBadge(paraResponder, naoLidas)
    }

    private fun identidadeOuFalhar(): SessionIdentity =
        syncCoordinator.currentIdentityOrNull() ?: throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, "Entre para usar Trocas.")

    private suspend fun <T> comToken(action: suspend (String) -> T): T = syncCoordinator.withFreshToken(action)
}
