package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TipoTurnoRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesRemoteDto
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.TurnosMesStatus
import br.com.leorvergani.escalaici.kmp.lab.firebase.dto.UsuarioRemoteDto
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Usuarios ativos + `turnosMes` PUBLICADA + catalogo de uma equipe/
 * competencia, carregados juntos - reaproveitado por Trocas (assistente de
 * nova solicitacao) e por "quem trabalha nesse dia" (FASE 16, secoes 17/22).
 */
data class TeamScheduleSnapshot(
    val equipeId: String,
    val competencia: String,
    val usuariosAtivos: List<UsuarioRemoteDto>,
    val turnosMesPublicadas: List<TurnosMesRemoteDto>,
    val catalogo: Map<String, TipoTurnoRemoteDto>,
) {
    private val turnosPorLogin: Map<String, TurnosMesRemoteDto> by lazy { turnosMesPublicadas.associateBy { it.login } }

    fun jornadaDoDia(login: String, dataIso: String): JornadaDia =
        resolverJornadaDia(turnosPorLogin[login]?.dias?.get(dataIso), catalogo, dataIso)

    /** Colegas ativos (exceto `loginAtual`) que trabalham nessa data - passo 2 do assistente de troca e "quem trabalha nesse dia". */
    fun colegasNoDia(loginAtual: String, dataIso: String): List<Pair<UsuarioRemoteDto, JornadaDia>> =
        usuariosAtivos
            .filter { it.login != loginAtual }
            .mapNotNull { usuario ->
                val jornada = jornadaDoDia(usuario.login, dataIso)
                if (jornada.trabalha) usuario to jornada else null
            }
}

/**
 * Mantem **um** snapshot em memoria por `equipeId+competencia` - nunca
 * reconsulta o Firestore a cada clique/recomposicao (ajuste obrigatorio da
 * FASE 16, aprovacao do usuario). Troca de equipe/competencia ou
 * `forceRefresh=true` e o unico jeito de buscar de novo; [invalidate] limpa
 * o cache sem buscar (usado no refresh manual do cabecalho).
 */
class TeamScheduleRepository(
    private val firestore: FirestoreRestClient,
    private val usuarioRepository: UsuarioRepository,
    private val tiposTurnoRepository: TiposTurnoRepository,
) {
    private val mutex = Mutex()
    private var cached: TeamScheduleSnapshot? = null

    suspend fun snapshot(
        idToken: String,
        equipeId: String,
        competencia: String,
        forceRefresh: Boolean = false,
    ): TeamScheduleSnapshot = mutex.withLock {
        val current = cached
        if (!forceRefresh && current != null && current.equipeId == equipeId && current.competencia == competencia) {
            return@withLock current
        }
        val usuarios = usuarioRepository.listarAtivosPorEquipe(idToken, equipeId)
        val turnosMes = turnosMesPublicadasDaEquipe(idToken, equipeId, competencia)
        val catalogo = tiposTurnoRepository.catalogoPara(idToken, equipeId)
        TeamScheduleSnapshot(equipeId, competencia, usuarios, turnosMes, catalogo).also { cached = it }
    }

    /** Limpa o cache sem buscar - a proxima chamada a [snapshot] recarrega. */
    suspend fun invalidate() {
        mutex.withLock { cached = null }
    }

    private suspend fun turnosMesPublicadasDaEquipe(idToken: String, equipeId: String, competencia: String): List<TurnosMesRemoteDto> {
        val documentos = try {
            firestore.runQuery(
                idToken,
                "turnosMes",
                listOf(
                    FieldEquals.Text("equipeId", equipeId),
                    FieldEquals.Text("competencia", competencia),
                    FieldEquals.Text("status", TurnosMesStatus.PUBLICADA.name),
                ),
            )
        } catch (e: FirestoreUnauthorizedException) {
            throw EscalaIciException(EscalaIciError.AUTH_REQUIRED, e.message ?: "Sessao expirada.")
        } catch (e: FirestorePermissionDeniedException) {
            throw EscalaIciException(EscalaIciError.PERMISSION_DENIED, e.message ?: "Sem permissao para consultar a escala da equipe.")
        } catch (e: FirestoreNetworkException) {
            throw EscalaIciException(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede ao consultar a escala da equipe.")
        }
        return documentos.mapNotNull(RemoteDtoMappers::turnosMes)
    }
}
