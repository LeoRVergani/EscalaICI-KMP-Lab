package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.auth.CorporateIdentity
import br.com.leorvergani.escalaici.identity.DemoWorkspaceOverview

internal data class DemoWorkspaceOverviewSession(
    val authenticatedDisplayName: String,
    val authenticatedLogin: String,
    val authenticatedEmail: String,
    val role: String,
    val overview: DemoWorkspaceOverview
)

internal sealed interface DemoWorkspaceEntryDecision {
    data class ShowOverview(val session: DemoWorkspaceOverviewSession) : DemoWorkspaceEntryDecision
    data class Denied(val message: String) : DemoWorkspaceEntryDecision
}

internal fun decideDemoWorkspaceEntry(
    identity: CorporateIdentity,
    authorized: Boolean,
    overview: DemoWorkspaceOverview
): DemoWorkspaceEntryDecision =
    if (authorized) {
        DemoWorkspaceEntryDecision.ShowOverview(
            DemoWorkspaceOverviewSession(
                authenticatedDisplayName = identity.displayName,
                authenticatedLogin = identity.username,
                authenticatedEmail = identity.email.orEmpty(),
                role = overview.developerRole,
                overview = overview
            )
        )
    } else {
        DemoWorkspaceEntryDecision.Denied("Esta conta não possui acesso ao modo Demo.")
    }
