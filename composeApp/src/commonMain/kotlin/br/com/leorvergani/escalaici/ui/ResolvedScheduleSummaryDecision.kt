package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.identity.ResolvedOrganizationContext
import br.com.leorvergani.escalaici.model.ScheduleSummary

internal const val ActiveScheduleMissingMessage = "Escala ativa não encontrada para esta conta."

internal data class ResolvedScheduleSummaryDecision(
    val summary: ScheduleSummary?,
    val errorMessage: String?
)

internal fun decideResolvedScheduleSummary(
    currentSummary: ScheduleSummary,
    resolvedContext: ResolvedOrganizationContext,
    loadPublishedScheduleSummaryAvailable: Boolean,
    publishedSummary: ScheduleSummary?
): ResolvedScheduleSummaryDecision {
    if (publishedSummary != null) {
        return ResolvedScheduleSummaryDecision(summary = publishedSummary, errorMessage = null)
    }
    if (loadPublishedScheduleSummaryAvailable) {
        return ResolvedScheduleSummaryDecision(summary = null, errorMessage = ActiveScheduleMissingMessage)
    }
    return ResolvedScheduleSummaryDecision(
        summary = currentSummary.withResolvedIdentity(resolvedContext),
        errorMessage = null
    )
}

/**
 * Sobrepõe identidade real (membro/equipe) num [ScheduleSummary] existente, sem tocar
 * nos turnos/dados de escala em si. Usado tanto quando não há carregador de publicação
 * configurado (fallback histórico acima) quanto no gate de login (App.kt) quando a
 * identidade resolve mas a publicação oficial ainda não existe - nesse segundo caso,
 * NUNCA deve travar a entrada no app (ver `sessionMemberId` em `EntryContext.LOGIN`),
 * só deixar a escala em si desatualizada até haver publicação de verdade.
 */
internal fun ScheduleSummary.withResolvedIdentity(resolvedContext: ResolvedOrganizationContext): ScheduleSummary =
    copy(
        member = member.copy(
            id = resolvedContext.memberId,
            displayName = resolvedContext.memberDisplayName,
            scaleName = resolvedContext.memberDisplayName
        ),
        team = team.copy(
            teamId = resolvedContext.primaryTeamId ?: team.teamId,
            name = resolvedContext.primaryTeamName ?: team.name,
            displayName = resolvedContext.primaryTeamName ?: team.displayName
        )
    )

internal data class LoginEntryOutcome(
    val sessionMemberId: String,
    val summary: ScheduleSummary,
    val shouldLoadChangeRequests: Boolean
)

/**
 * Decisão de entrada no app para `EntryContext.LOGIN` (App.kt) a partir de uma
 * identidade corporativa já resolvida ([OrganizationResolutionResult.Resolved]).
 *
 * Guarda de regressão (bug relatado 2026-07-24, corrigido nesta função): identidade
 * corporativa resolvida SEMPRE concede `sessionMemberId`, mesmo quando
 * [scheduleDecision] não trouxe uma escala publicada (`summary == null`, estado
 * vazio de [br.com.leorvergani.escalaici.source.ScheduleSyncCause.isEmptyState]).
 * Antes desta função existir, esse caminho zerava `sessionMemberId` e mostrava
 * "A escala oficial ainda não foi publicada" como erro bloqueante na tela de
 * login — o usuário autenticava via MSAL mas nunca via o app, mesmo sem
 * nenhum problema real de identidade/autorização. Publicação ausente deve, no
 * máximo, deixar a escala em si desatualizada; nunca barrar a entrada.
 */
internal fun decideLoginEntry(
    resolvedContext: ResolvedOrganizationContext,
    currentSummary: ScheduleSummary,
    scheduleDecision: ResolvedScheduleSummaryDecision
): LoginEntryOutcome =
    LoginEntryOutcome(
        sessionMemberId = resolvedContext.memberId,
        summary = scheduleDecision.summary ?: currentSummary.withResolvedIdentity(resolvedContext),
        shouldLoadChangeRequests = scheduleDecision.summary != null
    )
