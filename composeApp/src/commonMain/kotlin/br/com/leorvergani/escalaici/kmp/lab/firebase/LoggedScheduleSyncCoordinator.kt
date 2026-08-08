package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orquestra cache -> sessao -> usuario -> escala -> catalogo -> mapeamento
 * -> cache -> `StateFlow<ScheduleSyncState>` (prompt FASE 15 secao 16).
 * Zero configuracao do usuario: depois do login (ou com sessao restaurada),
 * a escala aparece sozinha - nenhuma tela pede equipe/mes/competencia.
 *
 * Se o token de acesso expirar no meio de uma sincronizacao (401 do
 * Firestore), renova uma vez e tenta de novo antes de reportar erro.
 */
class LoggedScheduleSyncCoordinator(
    private val authRepository: AuthRepository,
    private val usuarioRepository: UsuarioRepository,
    private val tiposTurnoRepository: TiposTurnoRepository,
    private val scheduleResolver: CurrentScheduleResolver,
    private val cache: EscalaIciScheduleCache,
    private val todayIsoProvider: () -> String,
) {
    private val _state = MutableStateFlow<ScheduleSyncState>(ScheduleSyncState.Idle)
    val state: StateFlow<ScheduleSyncState> = _state.asStateFlow()

    /** Chamar na abertura do app: restaura a sessao (se houver) e sincroniza - sem pedir nada ao usuario. */
    suspend fun start() {
        _state.value = ScheduleSyncState.RestoringSession
        val user = runCatching { authRepository.restoreSession() }.getOrNull()
        if (user == null) {
            _state.value = ScheduleSyncState.Error(EscalaIciError.AUTH_REQUIRED, "Entre para ver sua escala.")
            return
        }
        syncShowingCacheFirst(user.login)
    }

    suspend fun login(email: String, password: String) {
        _state.value = ScheduleSyncState.Authenticating
        val user = try {
            authRepository.login(email, password)
        } catch (e: EscalaIciException) {
            _state.value = ScheduleSyncState.Error(e.error, e.message)
            return
        }
        syncShowingCacheFirst(user.login)
    }

    /** Refresh manual/ao voltar ao foreground - nunca obrigatorio, cache continua valido se a rede falhar. */
    suspend fun refresh() {
        val login = currentLoginOrNull() ?: return
        sync(login)
    }

    /**
     * Executa `action` com o ID token atual; se falhar por sessao expirada
     * (401), renova uma vez e tenta de novo - mesmo padrao de retry de
     * [resolveOnce], generalizado para outros repositorios que nao sejam a
     * escala em si (Trocas, FASE 16). Nunca esconde um segundo 401 - so
     * tenta renovar uma vez.
     */
    suspend fun <T> withFreshToken(action: suspend (String) -> T): T {
        val token = authRepository.currentIdToken() ?: authRepository.ensureFreshIdToken()
        return try {
            action(token)
        } catch (e: EscalaIciException) {
            if (e.error != EscalaIciError.AUTH_REQUIRED) throw e
            action(authRepository.ensureFreshIdToken())
        }
    }

    /** Identidade da sessao atual (login/nome/ativo/equipeId/competencia) a partir do estado ja carregado - usado por [TrocasSession], nunca abre uma segunda sessao. */
    fun currentIdentityOrNull(): SessionIdentity? {
        val summary = (state.value as? ScheduleSyncState.Ready)?.summary ?: (state.value as? ScheduleSyncState.Cached)?.summary ?: return null
        return SessionIdentity(
            login = summary.member.id,
            nome = summary.member.displayName,
            ativo = summary.member.active,
            equipeId = summary.member.teamId,
            competencia = summary.competencia,
        )
    }

    suspend fun logout() {
        val login = currentLoginOrNull()
        authRepository.logout()
        login?.let(cache::clear)
        _state.value = ScheduleSyncState.Idle
    }

    private suspend fun currentLoginOrNull(): String? {
        (state.value as? ScheduleSyncState.Ready)?.let { return it.summary.member.id }
        (state.value as? ScheduleSyncState.Cached)?.let { return it.summary.member.id }
        return runCatching { authRepository.restoreSession() }.getOrNull()?.login
    }

    private suspend fun syncShowingCacheFirst(login: String) {
        cache.load(login)?.let { snapshot ->
            val summary = EscalaIciScheduleMapper.map(snapshot.turnosMes, snapshot.usuario, snapshot.catalogo)
            _state.value = ScheduleSyncState.Cached(summary, snapshot.sincronizadoEm)
        }
        sync(login)
    }

    private suspend fun sync(login: String) {
        val cachedSummary = (state.value as? ScheduleSyncState.Cached)?.summary
            ?: (state.value as? ScheduleSyncState.Ready)?.summary
            ?: cache.load(login)?.let { EscalaIciScheduleMapper.map(it.turnosMes, it.usuario, it.catalogo) }

        try {
            _state.value = ScheduleSyncState.ResolvingUser
            val hojeIso = todayIsoProvider()
            val (usuario, turnosMes, catalogo) = try {
                resolveOnce(login, hojeIso)
            } catch (expired: EscalaIciException) {
                if (expired.error != EscalaIciError.AUTH_REQUIRED) throw expired
                authRepository.ensureFreshIdToken()
                resolveOnce(login, hojeIso)
            }

            val summary = EscalaIciScheduleMapper.map(turnosMes, usuario, catalogo)
            cache.save(
                CachedEscalaSnapshot(
                    login = login,
                    equipeId = usuario.equipeId,
                    competencia = turnosMes.competencia,
                    periodoInicio = turnosMes.periodoInicio,
                    periodoFim = turnosMes.periodoFim,
                    atualizadoEmRemoto = turnosMes.atualizadoEm,
                    sincronizadoEm = hojeIso,
                    turnosMes = turnosMes,
                    catalogo = catalogo,
                    usuario = usuario,
                )
            )
            _state.value = ScheduleSyncState.Ready(summary, hojeIso)
        } catch (e: EscalaIciException) {
            _state.value = ScheduleSyncState.Error(e.error, e.message, cachedSummary)
        } catch (e: Exception) {
            _state.value = ScheduleSyncState.Error(EscalaIciError.NETWORK_ERROR, e.message ?: "Falha de rede.", cachedSummary)
        }
    }

    private suspend fun resolveOnce(login: String, hojeIso: String) = run {
        val idToken = authRepository.currentIdToken() ?: authRepository.ensureFreshIdToken()
        val usuario = usuarioRepository.resolveUsuario(idToken, login)
        _state.value = ScheduleSyncState.LoadingSchedule
        val turnosMes = scheduleResolver.resolve(idToken, login, usuario.equipeId, hojeIso)
        val catalogo = tiposTurnoRepository.catalogoPara(idToken, usuario.equipeId)
        Triple(usuario, turnosMes, catalogo)
    }
}
