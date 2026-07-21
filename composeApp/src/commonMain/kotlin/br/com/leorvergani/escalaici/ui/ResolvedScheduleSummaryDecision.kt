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
        summary = currentSummary.copy(
            member = currentSummary.member.copy(
                id = resolvedContext.memberId,
                displayName = resolvedContext.memberDisplayName,
                scaleName = resolvedContext.memberDisplayName
            ),
            team = currentSummary.team.copy(
                teamId = resolvedContext.primaryTeamId ?: currentSummary.team.teamId,
                name = resolvedContext.primaryTeamName ?: currentSummary.team.name,
                displayName = resolvedContext.primaryTeamName ?: currentSummary.team.displayName
            )
        ),
        errorMessage = null
    )
}
