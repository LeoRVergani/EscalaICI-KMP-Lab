package br.com.leorvergani.escalaici.kmp.lab.firebase

import br.com.leorvergani.escalaici.kmp.lab.model.ScheduleSummary

/** Erros minimos e distintos - nunca uma mensagem generica para tudo (prompt FASE 15 secao 28). */
enum class EscalaIciError {
    AUTH_REQUIRED,
    INVALID_CREDENTIALS,
    USER_NOT_FOUND,
    USER_INACTIVE,
    TEAM_NOT_FOUND,
    NO_PUBLISHED_SCHEDULE,
    NO_CURRENT_PERIOD,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    INVALID_REMOTE_DATA,
    CACHE_CORRUPTED,
    UNKNOWN_ERROR,

    // --- Trocas (FASE 16) ---
    TROCA_NOT_FOUND,
    TROCA_INVALID_TRANSITION,
    /** Documento mudou desde a leitura que originou a escrita (`FirestoreConflictException`) - releia antes de tentar novamente. */
    TROCA_CONFLICT,
    TROCA_DUPLICATE,
    /** `validarNovaSolicitacaoTroca` recusou a solicitação - mensagem já é a lista de motivos em texto amigável. */
    TROCA_VALIDATION_FAILED,
}

data class EscalaIciException(val error: EscalaIciError, override val message: String) : Exception(message)

/** Identidade minima da sessao logada, extraida do [ScheduleSummary] ja carregado - usada por Trocas (FASE 16) para nunca precisar de um segundo login. */
data class SessionIdentity(val login: String, val nome: String, val ativo: Boolean, val equipeId: String, val competencia: String)

/** Estado da sincronizacao logada. `Cached`/`Ready` carregam o mesmo `ScheduleSummary` que a UI ja consome. */
sealed interface ScheduleSyncState {
    data object Idle : ScheduleSyncState
    data object RestoringSession : ScheduleSyncState
    data object Authenticating : ScheduleSyncState
    data object ResolvingUser : ScheduleSyncState
    data object LoadingSchedule : ScheduleSyncState
    data class Cached(val summary: ScheduleSummary, val syncedAtLabel: String?) : ScheduleSyncState
    data class Ready(val summary: ScheduleSummary, val syncedAtLabel: String?) : ScheduleSyncState
    data class Error(val error: EscalaIciError, val message: String, val cachedSummary: ScheduleSummary? = null) : ScheduleSyncState
}
