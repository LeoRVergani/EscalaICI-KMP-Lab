package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.DiaRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca

/**
 * Porte puro (sem I/O, 100% testavel) de `lib/trocasEscala.ts` - transicoes,
 * rotulos/severidade e validacao de uma nova solicitacao. Espelha as
 * Firestore Rules (`trocasEscala`, bloco `allow update`) - mudar aqui sem
 * mudar la (ou vice-versa) quebra a fronteira entre "o que a UI tenta
 * fazer" e "o que o servidor aceita". Nao inclui `aplicarTrocaNosDias`/
 * `trocaDesatualizada` - usados so por `gestorAprovarEPublicarTroca`, fora
 * do escopo desta fase no KMP (secao 11 do prompt FASE 16).
 */
val TRANSICOES_TROCA_REAL: Map<StatusTroca, List<StatusTroca>> = mapOf(
    StatusTroca.PENDENTE_USUARIO to listOf(StatusTroca.RECUSADA_USUARIO, StatusTroca.PENDENTE_GESTOR, StatusTroca.CANCELADA_SOLICITANTE),
    StatusTroca.PENDENTE_GESTOR to listOf(StatusTroca.RECUSADA_GESTOR, StatusTroca.APROVADA_PUBLICADA),
    StatusTroca.RECUSADA_USUARIO to emptyList(),
    StatusTroca.CANCELADA_SOLICITANTE to emptyList(),
    StatusTroca.RECUSADA_GESTOR to emptyList(),
    StatusTroca.APROVADA_PUBLICADA to emptyList(),
    StatusTroca.EXPIRADA to emptyList(),
)

/** Status que ainda podem seguir adiante - usado tanto pela UI (abas Minhas/Para responder vs. Historico) quanto pela checagem de solicitacao duplicada. */
val STATUS_TROCA_ATIVOS: List<StatusTroca> = listOf(StatusTroca.PENDENTE_USUARIO, StatusTroca.PENDENTE_GESTOR)

val ROTULO_STATUS_TROCA: Map<StatusTroca, String> = mapOf(
    StatusTroca.PENDENTE_USUARIO to "Aguardando colega",
    StatusTroca.RECUSADA_USUARIO to "Recusada pelo colega",
    StatusTroca.CANCELADA_SOLICITANTE to "Cancelada",
    StatusTroca.PENDENTE_GESTOR to "Aguardando gestor",
    StatusTroca.RECUSADA_GESTOR to "Recusada pelo gestor",
    StatusTroca.APROVADA_PUBLICADA to "Concluída",
    StatusTroca.EXPIRADA to "Expirada",
)

enum class SeveridadeStatusTroca { SUCCESS, WARNING, DANGER, NEUTRAL }

val SEVERIDADE_STATUS_TROCA: Map<StatusTroca, SeveridadeStatusTroca> = mapOf(
    StatusTroca.PENDENTE_USUARIO to SeveridadeStatusTroca.WARNING,
    StatusTroca.RECUSADA_USUARIO to SeveridadeStatusTroca.DANGER,
    StatusTroca.CANCELADA_SOLICITANTE to SeveridadeStatusTroca.NEUTRAL,
    StatusTroca.PENDENTE_GESTOR to SeveridadeStatusTroca.WARNING,
    StatusTroca.RECUSADA_GESTOR to SeveridadeStatusTroca.DANGER,
    StatusTroca.APROVADA_PUBLICADA to SeveridadeStatusTroca.SUCCESS,
    StatusTroca.EXPIRADA to SeveridadeStatusTroca.NEUTRAL,
)

const val LIMITE_MENSAGEM_TROCA = 280

fun statusEhAtivo(status: StatusTroca): Boolean = status in STATUS_TROCA_ATIVOS

fun transicaoPermitida(de: StatusTroca, para: StatusTroca): Boolean =
    TRANSICOES_TROCA_REAL[de]?.contains(para) == true

data class ContextoValidacaoNovaTroca(
    val solicitanteLogin: String,
    val destinatarioLogin: String,
    val solicitanteAtivo: Boolean,
    val destinatarioAtivo: Boolean,
    /** `null` replica `Dia | undefined` do TS - dia inexistente no mapa `dias`, nao uma checagem de `trabalha`. */
    val diaSolicitante: DiaRemoteDto?,
    val diaDestinatario: DiaRemoteDto?,
)

/**
 * Mesma cobertura de `validarNovaSolicitacaoTroca` (TS) - regras que
 * dependem dos dois `Dia` (existencia e codigo), que as Firestore Rules nao
 * conseguem ler porque exigiriam `get()` cruzado em `turnosMes`.
 */
fun validarNovaSolicitacaoTroca(contexto: ContextoValidacaoNovaTroca): List<String> {
    val erros = mutableListOf<String>()

    if (contexto.destinatarioLogin.isBlank()) {
        erros += "Informe o colaborador que receberá a solicitação."
    } else if (contexto.destinatarioLogin == contexto.solicitanteLogin) {
        erros += "Escolha outro colaborador para a troca."
    }
    if (!contexto.solicitanteAtivo) {
        erros += "O solicitante precisa estar ativo."
    }
    if (!contexto.destinatarioAtivo) {
        erros += "O destinatário precisa estar ativo."
    }
    if (contexto.diaSolicitante == null) {
        erros += "Você não tem turno nesse dia."
    }
    if (contexto.diaDestinatario == null) {
        erros += "O colega não tem turno nesse dia."
    }
    if (contexto.diaSolicitante != null && contexto.diaDestinatario != null && contexto.diaSolicitante.c == contexto.diaDestinatario.c) {
        erros += "Os dois já estão no mesmo turno nesse dia — não há o que trocar."
    }

    return erros
}

/**
 * Badge de Trocas (FASE 16, seção 21) - `total` conta **itens de atenção
 * únicos**, não a soma bruta de troca+notificação. `paraResponder` é a
 * contagem de trocas acionáveis (fonte primária); `notificacoesDistintas`
 * é só o que sobra de não lidas que não corresponde a nenhuma delas (ex.:
 * "sua troca foi aceita" para o solicitante, quando a troca já avançou para
 * `PENDENTE_GESTOR` e por isso não é mais "acionável" para esse login).
 */
data class TrocasBadge(val paraResponder: Int, val notificacoesDistintas: Int) {
    val total: Int get() = paraResponder + notificacoesDistintas
}

/**
 * Deduplica por `trocaId`: uma troca `PENDENTE_USUARIO` destinada a
 * `loginAtual` e a notificação `TROCA_SOLICITADA` (não lida) da mesma troca
 * representam **um único** item de atenção, não dois. Notificações não
 * lidas cujo `trocaId` não corresponde a nenhuma troca acionável (ex.:
 * "troca aceita, aguardando gestor" para quem solicitou) contam como um
 * item distinto - comportamento documentado, não é bug: ainda é algo que o
 * usuário não viu.
 */
fun calcularTrocasBadge(
    loginAtual: String,
    trocas: List<SolicitacaoTrocaRealDto>,
    notificacoes: List<NotificacaoTrocaDto>,
): TrocasBadge {
    val trocaIdsAcionaveis = trocas.asSequence()
        .filter { it.destinatarioLogin == loginAtual && it.status == StatusTroca.PENDENTE_USUARIO }
        .map { it.trocaId }
        .toSet()
    val notificacoesDistintas = notificacoes.count { it.lidaEm == null && it.trocaId !in trocaIdsAcionaveis }
    return TrocasBadge(paraResponder = trocaIdsAcionaveis.size, notificacoesDistintas = notificacoesDistintas)
}

/**
 * Notificações NÃO lidas de uma troca específica - usado ao abrir uma troca
 * para marcar como lida só o que é relevante àquela troca, nunca todas as
 * notificações ao simplesmente entrar na aba Trocas (FASE 16, hardening
 * item 1).
 */
fun notificacoesNaoLidasDaTroca(notificacoes: List<NotificacaoTrocaDto>, trocaId: String): List<NotificacaoTrocaDto> =
    notificacoes.filter { it.trocaId == trocaId && it.lidaEm == null }
