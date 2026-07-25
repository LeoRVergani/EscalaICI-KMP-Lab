package br.com.leorvergani.escalaici.diagnostics

import br.com.leorvergani.escalaici.source.ScheduleSyncCause
import br.com.leorvergani.escalaici.source.classifySyncFailure

/**
 * Diagnostico seguro de uma falha na cadeia de resolucao (identidade corporativa/publicacao) -
 * NUNCA inclui token, header Authorization, API key, chave privada ou dado pessoal completo.
 * So o suficiente para saber onde e por que a cadeia falhou (spec 67, Checkpoint H addendum):
 * antes desta classe, `DemoPublicationResolver.loadOneAttempt()` engolia qualquer `Throwable`
 * silenciosamente - identidade corporativa podia falhar sem nenhuma pista no logcat/console.
 */
data class ResolutionFailureDiagnostic(
    val step: String,
    val workspaceId: String,
    val cause: ScheduleSyncCause,
    val exceptionType: String,
    val httpStatus: Int?,
    val sanitizedMessage: String
)

expect fun logResolutionFailure(diagnostic: ResolutionFailureDiagnostic)

/**
 * Trace seguro (nivel info, nao-erro) de uma transicao da maquina de estados de
 * sessao/identidade/publicacao (spec 67, Checkpoint I). Chamador e responsavel por nunca passar
 * token, header `Authorization`, API key, chave privada, e-mail completo, object ID completo ou
 * tenant ID completo - so nomes de classe/estado, contagens e booleans. Usado para reconstruir a
 * sequencia real (login -> Hoje -> logout -> login) sem depender so de leitura de codigo.
 */
expect fun logResolutionTrace(message: String)

/** Extrai um status HTTP de uma mensagem de erro tipo "... (404) ...", sem expor mais nada dela. */
internal fun extractHttpStatus(message: String?): Int? {
    if (message.isNullOrBlank()) return null
    val parenMatch = Regex("""\((\d{3})\)""").find(message)
    if (parenMatch != null) return parenMatch.groupValues[1].toIntOrNull()
    val bareMatch = Regex("""\b([45]\d{2})\b""").find(message)
    return bareMatch?.groupValues?.get(1)?.toIntOrNull()
}

/**
 * Constroi o diagnostico a partir da excecao real capturada - a UNICA funcao que ve
 * `throwable.message` por inteiro; tudo que sai dela ja e seguro para log (classificacao +
 * tipo da excecao + status HTTP quando extraivel + mensagem ja sanitizada por quem chama).
 */
fun buildResolutionFailureDiagnostic(
    step: String,
    workspaceId: String,
    throwable: Throwable,
    sanitizedMessage: String
): ResolutionFailureDiagnostic {
    val cause = classifySyncFailure(throwable)
    return ResolutionFailureDiagnostic(
        step = step,
        workspaceId = workspaceId,
        cause = cause,
        exceptionType = throwable::class.simpleName ?: "Unknown",
        httpStatus = extractHttpStatus(throwable.message),
        sanitizedMessage = sanitizedMessage
    )
}
