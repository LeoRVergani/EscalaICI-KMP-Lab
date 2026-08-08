package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus

/** Resolve a `TurnosMes` PUBLICADA vigente para hoje, sem o usuario escolher competencia (prompt FASE 15 secao 10). */
interface CurrentScheduleResolver {
    suspend fun resolve(idToken: String, login: String, equipeId: String, hojeIso: String): TurnosMesRemoteDto
}

/**
 * Duas etapas, nessa ordem:
 * 1. **Caminho rapido**: `GET` direto pelo ID deterministico
 *    (`${equipeId}_${login}_${competenciaOperacional}`) - evita uma query
 *    sempre que a competencia operacional (corte no dia 26, ver [Jornada])
 *    ja for a certa, cobrindo o caso comum sem custo de query.
 * 2. **Consulta** (`login==X AND equipeId==Y AND status=='PUBLICADA'`, so
 *    igualdades - sem indice composto novo) quando o caminho rapido nao
 *    bate exatamente no periodo de hoje - cobre ciclos que atravessam o
 *    mes (26/mes a 25/mes+1) e o corte do dia 26.
 *
 * Divergencia deliberada do app real (`selecionarEscalaPorData`, que cai
 * para "a competencia mais recente disponivel" como ultimo recurso): aqui,
 * se nenhuma escala publicada cobrir literalmente hoje nem bater com a
 * competencia operacional, o resultado e [EscalaIciError.NO_CURRENT_PERIOD]
 * em vez de mostrar silenciosamente uma escala de outro periodo - exigido
 * pelos criterios de aceite da FASE 15 (periodo anterior/futuro nunca
 * selecionado como atual).
 */
class FirestoreCurrentScheduleResolver(
    private val firestore: FirestoreRestClient,
) : CurrentScheduleResolver {

    override suspend fun resolve(idToken: String, login: String, equipeId: String, hojeIso: String): TurnosMesRemoteDto {
        val operacional = Jornada.competenciaOperacional(hojeIso)

        val viaId = publicadaPorId(idToken, equipeId, login, operacional)
        if (viaId != null && viaId.periodoInicio <= hojeIso && hojeIso <= viaId.periodoFim) {
            return viaId
        }

        val candidatas = publicadasDoUsuario(idToken, login, equipeId)
        if (candidatas.isEmpty() && viaId == null) {
            throw EscalaIciException(EscalaIciError.NO_PUBLISHED_SCHEDULE, "Nenhuma escala publicada foi encontrada para o seu login.")
        }

        return escolherEscalaAtual(candidatas, viaId, hojeIso, operacional)
            ?: throw EscalaIciException(EscalaIciError.NO_CURRENT_PERIOD, "Nenhuma escala publicada cobre o periodo atual.")
    }

    private suspend fun publicadaPorId(idToken: String, equipeId: String, login: String, competencia: String): TurnosMesRemoteDto? {
        val docId = idDocumentoTurnosMes(equipeId, login, competencia)
        val document = try {
            firestore.getDocument(idToken, "turnosMes", docId)
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            // As Rules do Firestore devolvem 403 (nao 404) tanto para "documento
            // nao existe" quanto para "existe mas e de outra equipe" quando a
            // regra referencia `resource.data` - um ID adivinhado (competencia
            // operacional) errado e o caso comum, entao aqui isso e tratado como
            // "nao encontrado por este caminho", nunca abortando a resolucao -
            // a query em `publicadasDoUsuario` e quem decide se e de fato
            // PERMISSION_DENIED. Bug real encontrado testando contra staging.
            return null
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao ler a escala.")
        } ?: return null
        val turnosMes = RemoteDtoMappers.turnosMes(document) ?: return null
        return turnosMes.takeIf { it.status == TurnosMesStatus.PUBLICADA }
    }

    private suspend fun publicadasDoUsuario(idToken: String, login: String, equipeId: String): List<TurnosMesRemoteDto> {
        val documentos = try {
            firestore.runQuery(
                idToken,
                "turnosMes",
                listOf(
                    FieldEquals.Text("login", login),
                    FieldEquals.Text("equipeId", equipeId),
                    FieldEquals.Text("status", "PUBLICADA"),
                ),
            )
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissao para consultar a escala.")
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao consultar a escala.")
        }
        return documentos.mapNotNull(RemoteDtoMappers::turnosMes)
    }
}

/** `${equipeId}_${login}_${competencia}` - equivalente exato de `idDocumento()` (`packages/contrato/src/documentos.ts`) do contrato real, ja com `login` (nao UID) como segundo componente. */
fun idDocumentoTurnosMes(equipeId: String, login: String, competencia: String): String = "${equipeId}_${login}_$competencia"

/**
 * Decisao pura (sem IO) de qual `TurnosMes` e "a atual" - extraida para ser
 * testada diretamente (prompt FASE 15 secao 32: periodo anterior/futuro/
 * atravessando mes). `viaId` (resultado do GET pelo id deterministico, pode
 * ser nulo) e mesclado aos `candidatas` (resultado da query) antes de
 * escolher, sem duplicar por competencia.
 */
fun escolherEscalaAtual(
    candidatas: List<TurnosMesRemoteDto>,
    viaId: TurnosMesRemoteDto?,
    hojeIso: String,
    competenciaOperacional: String,
): TurnosMesRemoteDto? {
    val todas = if (viaId != null) (candidatas + viaId).distinctBy { it.competencia } else candidatas
    val porPeriodo = todas.firstOrNull { it.periodoInicio <= hojeIso && hojeIso <= it.periodoFim }
    val porCompetenciaOperacional = todas.firstOrNull { it.competencia == competenciaOperacional }
    return porPeriodo ?: porCompetenciaOperacional
}
