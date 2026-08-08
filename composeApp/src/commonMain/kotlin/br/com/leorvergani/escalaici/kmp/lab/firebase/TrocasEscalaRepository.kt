package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.AtorTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.EventoHistoricoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.NotificacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SnapshotValidacaoTrocaDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.SolicitacaoTrocaRealDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.StatusTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoNotificacaoTroca
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.platform.nowIso
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Identidade minima de um participante da troca - evita acoplar a assinatura dos metodos a `UsuarioRemoteDto` inteiro. */
data class ParticipanteTroca(val login: String, val nome: String, val ativo: Boolean)

data class EntradaCriarSolicitacaoTroca(
    val equipeId: String,
    val competencia: String,
    val data: String,
    val solicitante: ParticipanteTroca,
    val destinatario: ParticipanteTroca,
    val mensagem: String,
    val catalogo: Map<String, TipoTurnoRemoteDto>,
)

/**
 * Le/escreve `trocasEscala`/`notificacoesTroca` - porte de
 * `lib/firebase/trocasRepository.ts` para o escopo do colaborador (secao 11
 * do prompt FASE 16): criar, cancelar, responder, marcar notificacao como
 * lida. Nunca usa `runTransaction`/aprovacao de gestor (fora de escopo) nem
 * grava em `turnosMes` - so `commit()` em `trocasEscala`/
 * `notificacoesTroca`, exatamente o que as Rules permitem ao colaborador
 * comum.
 */
class TrocasEscalaRepository(
    private val firestore: FirestoreRestClient,
) {

    suspend fun buscarMinhasTrocas(idToken: String, equipeId: String, competencia: String, login: String): List<SolicitacaoTrocaRealDto> {
        val comoSolicitante = consultarTrocas(idToken, equipeId, competencia, FieldEquals.Text("solicitanteLogin", login))
        val comoDestinatario = consultarTrocas(idToken, equipeId, competencia, FieldEquals.Text("destinatarioLogin", login))
        return (comoSolicitante + comoDestinatario)
            .distinctBy { it.trocaId }
            .sortedByDescending { it.atualizadoEm }
    }

    suspend fun buscarNotificacoes(idToken: String, login: String): List<NotificacaoTrocaDto> {
        val documentos = runFirestore("consultar notificações") {
            firestore.runQuery(idToken, "notificacoesTroca", listOf(FieldEquals.Text("destinatarioLogin", login)))
        }
        return documentos.mapNotNull(TrocasRemoteMappers::notificacaoTroca).sortedByDescending { it.criadoEm }
    }

    /**
     * Retorna o `trocaId` criado. Espelha `criarSolicitacaoTroca` (TS):
     * refetch fresco dos dois `turnosMes` (nunca reaproveita um
     * [TeamScheduleSnapshot] em cache para a escrita), valida, checa
     * duplicidade best-effort, grava troca+notificação no mesmo commit.
     */
    suspend fun criarSolicitacao(idToken: String, entrada: EntradaCriarSolicitacaoTroca): String {
        val idSolicitante = idDocumentoTurnosMes(entrada.equipeId, entrada.solicitante.login, entrada.competencia)
        val idDestinatario = idDocumentoTurnosMes(entrada.equipeId, entrada.destinatario.login, entrada.competencia)

        val docSolicitante = runFirestore("ler sua escala") { firestore.getDocument(idToken, "turnosMes", idSolicitante) }
            ?.let(RemoteDtoMappers::turnosMes)
            ?: throw EscalaIciException(EscalaIciError.NO_PUBLISHED_SCHEDULE, "Escala publicada não encontrada para você.")
        val docDestinatario = runFirestore("ler a escala do colega") { firestore.getDocument(idToken, "turnosMes", idDestinatario) }
            ?.let(RemoteDtoMappers::turnosMes)
            ?: throw EscalaIciException(EscalaIciError.NO_PUBLISHED_SCHEDULE, "Escala publicada não encontrada para o colega.")

        val diaSolicitante = docSolicitante.dias[entrada.data]
        val diaDestinatario = docDestinatario.dias[entrada.data]

        val erros = validarNovaSolicitacaoTroca(
            ContextoValidacaoNovaTroca(
                solicitanteLogin = entrada.solicitante.login,
                destinatarioLogin = entrada.destinatario.login,
                solicitanteAtivo = entrada.solicitante.ativo,
                destinatarioAtivo = entrada.destinatario.ativo,
                diaSolicitante = diaSolicitante,
                diaDestinatario = diaDestinatario,
            ),
        )
        if (erros.isNotEmpty()) {
            throw EscalaIciException(EscalaIciError.TROCA_VALIDATION_FAILED, erros.joinToString(" "))
        }

        val existentes = consultarTrocas(idToken, entrada.equipeId, entrada.competencia, FieldEquals.Text("solicitanteLogin", entrada.solicitante.login))
        val duplicada = existentes.any { it.data == entrada.data && statusEhAtivo(it.status) }
        if (duplicada) {
            throw EscalaIciException(EscalaIciError.TROCA_DUPLICATE, "Você já tem uma solicitação em andamento para esse dia.")
        }

        val jornadaSolicitante = resolverJornadaDia(diaSolicitante, entrada.catalogo, entrada.data)
        val jornadaDestinatario = resolverJornadaDia(diaDestinatario, entrada.catalogo, entrada.data)
        val trocaId = novoId()
        val agora = nowIso()

        val troca = SolicitacaoTrocaRealDto(
            trocaId = trocaId,
            equipeId = entrada.equipeId,
            competencia = entrada.competencia,
            solicitanteLogin = entrada.solicitante.login,
            solicitanteNome = entrada.solicitante.nome,
            destinatarioLogin = entrada.destinatario.login,
            destinatarioNome = entrada.destinatario.nome,
            data = entrada.data,
            turnoSolicitanteAntes = jornadaSolicitante.codigo,
            horarioSolicitanteAntes = jornadaSolicitante.horario,
            turnoDestinatarioAntes = jornadaDestinatario.codigo,
            horarioDestinatarioAntes = jornadaDestinatario.horario,
            status = StatusTroca.PENDENTE_USUARIO,
            mensagemSolicitante = entrada.mensagem.trim().ifBlank { null },
            motivoRecusa = null,
            criadoEm = agora,
            atualizadoEm = agora,
            respondidoEm = null,
            aprovadoEm = null,
            publicadoEm = null,
            gestorLogin = null,
            gestorNome = null,
            historico = listOf(
                EventoHistoricoTrocaDto(
                    tipo = "SOLICITACAO_CRIADA",
                    porLogin = entrada.solicitante.login,
                    porNome = entrada.solicitante.nome,
                    porPerfil = AtorTroca.SOLICITANTE,
                    em = agora,
                    descricao = "Solicitação criada",
                ),
            ),
            snapshotValidacao = SnapshotValidacaoTrocaDto(
                solicitanteDocId = idSolicitante,
                destinatarioDocId = idDestinatario,
                turnoSolicitanteOriginal = jornadaSolicitante.codigo,
                turnoDestinatarioOriginal = jornadaDestinatario.codigo,
            ),
        )

        val notificacao = novaNotificacao(
            destinatarioLogin = entrada.destinatario.login,
            equipeId = entrada.equipeId,
            tipo = TipoNotificacaoTroca.TROCA_SOLICITADA,
            titulo = "Nova solicitação de troca",
            mensagem = "${entrada.solicitante.nome} quer trocar o turno do dia ${entrada.data} com você.",
            trocaId = trocaId,
            criadoPorLogin = entrada.solicitante.login,
            em = agora,
        )

        val writes = buildList {
            add(FirestoreWrite.Create("trocasEscala", trocaId, TrocasRemoteMappers.encodeSolicitacaoTroca(troca)))
            notificacao?.let { add(FirestoreWrite.Create("notificacoesTroca", it.id, TrocasRemoteMappers.encodeNotificacaoTroca(it))) }
        }
        runFirestore("criar a solicitação de troca") { firestore.commit(idToken, writes) }
        return trocaId
    }

    /** `PENDENTE_USUARIO -> CANCELADA_SOLICITANTE` - so quem solicitou. */
    suspend fun cancelar(idToken: String, trocaId: String, solicitante: ParticipanteTroca) {
        val troca = buscarTrocaOuFalhar(idToken, trocaId)
        if (troca.solicitanteLogin != solicitante.login) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, "Só quem solicitou a troca pode cancelá-la.")
        }
        garantirTransicaoOuFalhar(troca.status, StatusTroca.CANCELADA_SOLICITANTE)

        val agora = nowIso()
        val historico = troca.historico + EventoHistoricoTrocaDto(
            tipo = "CANCELADA_SOLICITANTE",
            porLogin = solicitante.login,
            porNome = solicitante.nome,
            porPerfil = AtorTroca.SOLICITANTE,
            em = agora,
            descricao = "Cancelada pelo solicitante",
        )
        val notificacao = novaNotificacao(
            destinatarioLogin = troca.destinatarioLogin,
            equipeId = troca.equipeId,
            tipo = TipoNotificacaoTroca.TROCA_CANCELADA,
            titulo = "Solicitação de troca cancelada",
            mensagem = "${solicitante.nome} cancelou a solicitação de troca do dia ${troca.data}.",
            trocaId = trocaId,
            criadoPorLogin = solicitante.login,
            em = agora,
        )

        val writes = buildList {
            add(
                FirestoreWrite.Patch(
                    "trocasEscala",
                    trocaId,
                    TrocasRemoteMappers.encodeTrocaPatch(StatusTroca.CANCELADA_SOLICITANTE, agora, historico),
                    listOf("status", "atualizadoEm", "historico"),
                ),
            )
            notificacao?.let { add(FirestoreWrite.Create("notificacoesTroca", it.id, TrocasRemoteMappers.encodeNotificacaoTroca(it))) }
        }
        runFirestore("cancelar a solicitação") { firestore.commit(idToken, writes) }
    }

    /** Aceitar: `PENDENTE_USUARIO -> PENDENTE_GESTOR`. Recusar: `PENDENTE_USUARIO -> RECUSADA_USUARIO`. So o destinatario convidado. */
    suspend fun responder(idToken: String, trocaId: String, destinatario: ParticipanteTroca, aceitar: Boolean, motivoRecusa: String? = null) {
        val troca = buscarTrocaOuFalhar(idToken, trocaId)
        if (troca.destinatarioLogin != destinatario.login) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, "Só o colega convidado pode responder esta troca.")
        }
        val novoStatus = if (aceitar) StatusTroca.PENDENTE_GESTOR else StatusTroca.RECUSADA_USUARIO
        garantirTransicaoOuFalhar(troca.status, novoStatus)

        val agora = nowIso()
        val motivoFinal = if (aceitar) null else (motivoRecusa?.trim()?.ifBlank { null } ?: "Recusada pelo colega.")
        val historico = troca.historico + EventoHistoricoTrocaDto(
            tipo = if (aceitar) "ACEITE_DESTINATARIO" else "RECUSA_DESTINATARIO",
            porLogin = destinatario.login,
            porNome = destinatario.nome,
            porPerfil = AtorTroca.DESTINATARIO,
            em = agora,
            descricao = if (aceitar) {
                "Aceite do colega — encaminhada para o gestor"
            } else {
                "Recusada pelo colega" + (motivoRecusa?.trim()?.takeIf { it.isNotBlank() }?.let { ": $it" } ?: "")
            },
        )
        val notificacao = novaNotificacao(
            destinatarioLogin = troca.solicitanteLogin,
            equipeId = troca.equipeId,
            tipo = if (aceitar) TipoNotificacaoTroca.TROCA_ACEITA_AGUARDANDO_GESTOR else TipoNotificacaoTroca.TROCA_RECUSADA_USUARIO,
            titulo = if (aceitar) "Troca aceita, aguardando o gestor" else "Troca recusada pelo colega",
            mensagem = if (aceitar) {
                "${destinatario.nome} aceitou a troca do dia ${troca.data}. Agora o gestor precisa aprovar."
            } else {
                "${destinatario.nome} recusou a troca do dia ${troca.data}."
            },
            trocaId = trocaId,
            criadoPorLogin = destinatario.login,
            em = agora,
        )

        val writes = buildList {
            add(
                FirestoreWrite.Patch(
                    "trocasEscala",
                    trocaId,
                    TrocasRemoteMappers.encodeTrocaPatch(novoStatus, agora, historico, respondidoEm = agora, motivoRecusa = motivoFinal),
                    listOf("status", "atualizadoEm", "respondidoEm", "motivoRecusa", "historico"),
                ),
            )
            notificacao?.let { add(FirestoreWrite.Create("notificacoesTroca", it.id, TrocasRemoteMappers.encodeNotificacaoTroca(it))) }
        }
        runFirestore("responder a solicitação") { firestore.commit(idToken, writes) }
    }

    /** Toca **so** `lidaEm` - ajuste obrigatorio da FASE 16 (aprovacao do usuario): o badge nao pode ficar aceso para sempre. */
    suspend fun marcarNotificacaoComoLida(idToken: String, notificacaoId: String) {
        val write = FirestoreWrite.Patch(
            "notificacoesTroca",
            notificacaoId,
            TrocasRemoteMappers.encodeNotificacaoLidaPatch(nowIso()),
            listOf("lidaEm"),
        )
        runFirestore("marcar a notificação como lida") { firestore.commit(idToken, listOf(write)) }
    }

    private suspend fun buscarTrocaOuFalhar(idToken: String, trocaId: String): SolicitacaoTrocaRealDto {
        val document = runFirestore("ler a solicitação de troca") { firestore.getDocument(idToken, "trocasEscala", trocaId) }
            ?: throw EscalaIciException(EscalaIciError.TROCA_NOT_FOUND, "Solicitação de troca não encontrada.")
        return TrocasRemoteMappers.solicitacaoTroca(document)
            ?: throw EscalaIciException(EscalaIciError.INVALID_REMOTE_DATA, "Solicitação de troca com dados inválidos.")
    }

    private fun garantirTransicaoOuFalhar(de: StatusTroca, para: StatusTroca) {
        if (!transicaoPermitida(de, para)) {
            throw EscalaIciException(EscalaIciError.TROCA_INVALID_TRANSITION, "Transição de $de para $para não é permitida.")
        }
    }

    private suspend fun consultarTrocas(idToken: String, equipeId: String, competencia: String, filtroPapel: FieldEquals): List<SolicitacaoTrocaRealDto> {
        val documentos = runFirestore("consultar trocas") {
            firestore.runQuery(
                idToken,
                "trocasEscala",
                listOf(FieldEquals.Text("equipeId", equipeId), FieldEquals.Text("competencia", competencia), filtroPapel),
            )
        }
        return documentos.mapNotNull(TrocasRemoteMappers::solicitacaoTroca)
    }

    /** `null` quando o destinatario e quem executou a acao - Rules rejeitam `destinatarioLogin == criadoPorLogin` em `notificacoesTroca.create`, igual `criarNotificacaoTroca` (TS). */
    private fun novaNotificacao(
        destinatarioLogin: String,
        equipeId: String,
        tipo: TipoNotificacaoTroca,
        titulo: String,
        mensagem: String,
        trocaId: String,
        criadoPorLogin: String,
        em: String,
    ): NotificacaoTrocaDto? {
        if (destinatarioLogin == criadoPorLogin) return null
        return NotificacaoTrocaDto(
            id = novoId(),
            destinatarioLogin = destinatarioLogin,
            equipeId = equipeId,
            tipo = tipo,
            titulo = titulo,
            mensagem = mensagem,
            trocaId = trocaId,
            criadoPorLogin = criadoPorLogin,
            criadoEm = em,
            lidaEm = null,
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun novoId(): String = Uuid.random().toString()

    private suspend fun <T> runFirestore(acao: String, block: suspend () -> T): T = try {
        block()
    } catch (e: FirestoreUnauthorizedException) {
        throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessão expirada.")
    } catch (e: FirestorePermissionDeniedException) {
        throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissão para $acao.")
    } catch (e: FirestoreConflictException) {
        throw EscalaIciException(EscalaIciError.TROCA_CONFLICT, e.message ?: "A solicitação foi alterada por outra operação.")
    } catch (e: FirestoreNetworkException) {
        throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao $acao.")
    }
}
