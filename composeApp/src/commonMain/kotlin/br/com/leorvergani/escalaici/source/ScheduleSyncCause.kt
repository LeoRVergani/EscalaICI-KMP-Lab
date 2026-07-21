package br.com.leorvergani.escalaici.source

/**
 * Causa tipada de um estado não-sucesso de [DataLoadResult], complementar ao
 * texto livre em `message`/`SourceMetadata.userMessage` (que continua
 * existindo para exibição). Introduzido na ONDA 3.1 do sprint noturno de
 * 2026-07-15 para começar a substituir mensagens genéricas por causas
 * específicas — ver `docs/spec/48-ESCALAICI-SINCRONIZACAO-ESCALA-CACHE-OFFLINE.md`,
 * seção "Estados tipados de erro".
 *
 * TEAM_NOT_FOUND/NO_ACTIVE_PERIOD/NO_ASSIGNMENTS são determinados com certeza
 * (checados diretamente contra o que o gateway retornou). NETWORK_ERROR/
 * PERMISSION_DENIED/INVALID_REMOTE_DATA são heurísticos — inferidos do texto
 * da exceção capturada, pois nem toda plataforma expõe aqui um tipo de
 * exceção estruturado; quando a heurística não reconhece a exceção, o
 * resultado cai em UNKNOWN em vez de arriscar uma causa errada.
 * AUTH_REQUIRED e IDENTITY_NOT_LINKED não são produzidos por nenhum código
 * ainda (não há sessão Firebase Auth própria neste app — ver
 * `docs/ADR-FIREBASE-AUTH.md`); existem aqui só como os estados já previstos
 * no spec, para quando essa parte for implementada.
 */
enum class ScheduleSyncCause {
    AUTH_REQUIRED,
    IDENTITY_NOT_LINKED,
    TEAM_NOT_FOUND,
    NO_ACTIVE_PERIOD,
    NO_ASSIGNMENTS,
    PERMISSION_DENIED,
    NETWORK_ERROR,
    INVALID_REMOTE_DATA,
    CACHE_AVAILABLE,
    UNKNOWN,
}

/** É um estado de "nada para mostrar ainda" (equipe/período/turnos), não uma falha — nunca deve ser exibido como erro em vermelho. */
fun ScheduleSyncCause.isEmptyState(): Boolean =
    this == ScheduleSyncCause.TEAM_NOT_FOUND || this == ScheduleSyncCause.NO_ACTIVE_PERIOD || this == ScheduleSyncCause.NO_ASSIGNMENTS

fun ScheduleSyncCause.defaultMessage(teamId: String? = null): String = when (this) {
    ScheduleSyncCause.AUTH_REQUIRED -> "É necessário fazer login para ver a escala."
    ScheduleSyncCause.IDENTITY_NOT_LINKED -> "Vínculo de identidade corporativa: recurso ainda não implementado neste app."
    ScheduleSyncCause.TEAM_NOT_FOUND -> "Equipe${teamId?.let { " \"$it\"" } ?: ""} não encontrada no Firebase."
    ScheduleSyncCause.NO_ACTIVE_PERIOD -> "Esta equipe não tem um período de escala ativo no momento."
    ScheduleSyncCause.NO_ASSIGNMENTS -> "O período ativo desta equipe ainda não tem nenhum turno cadastrado."
    ScheduleSyncCause.PERMISSION_DENIED -> "Sem permissão para acessar esta escala no Firebase."
    ScheduleSyncCause.NETWORK_ERROR -> "Não foi possível conectar ao Firebase. Verifique sua internet e tente novamente."
    ScheduleSyncCause.INVALID_REMOTE_DATA -> "Os dados recebidos do Firebase estão em um formato inesperado."
    ScheduleSyncCause.CACHE_AVAILABLE -> "Mostrando a última escala salva localmente."
    ScheduleSyncCause.UNKNOWN -> "Não foi possível atualizar a escala. Tente novamente."
}

/**
 * Classificação heurística de uma exceção capturada durante a sincronização.
 * Nunca lança — na dúvida, retorna [ScheduleSyncCause.UNKNOWN] em vez de
 * arriscar uma causa incorreta (ex.: rotular um erro de rede como permissão
 * negada).
 */
fun classifySyncFailure(throwable: Throwable): ScheduleSyncCause {
    val text = "${throwable::class.simpleName.orEmpty()} ${throwable.message.orEmpty()}".lowercase()
    val permissionMarkers = listOf("permission_denied", "permission denied", "forbidden", "unauthenticated", "403", "permissão negada", "não autorizado")
    val authMarkers = listOf("not configured", "missing idtoken", "401")
    val networkMarkers = listOf("unavailable", "timeout", "timed out", "network", "unknownhost", "no address associated", "failed to connect", "econnrefused", "socket", "indisponível", "sem conexão", "sem internet")
    val invalidDataMarkers = listOf("invalid", "malformed", "parse", "unexpected", "serializ", "inválid", "formato inesperado", "divergente", "sem publicationrevision", "sem workspaceid", "status nao ativo", "revisao ativa positiva")
    return when {
        permissionMarkers.any { it in text } -> ScheduleSyncCause.PERMISSION_DENIED
        authMarkers.any { it in text } -> ScheduleSyncCause.AUTH_REQUIRED
        networkMarkers.any { it in text } -> ScheduleSyncCause.NETWORK_ERROR
        invalidDataMarkers.any { it in text } -> ScheduleSyncCause.INVALID_REMOTE_DATA
        else -> ScheduleSyncCause.UNKNOWN
    }
}
