package br.com.leorvergani.escalaici.ui

internal enum class DemoBackNavigationTarget {
    ENTRY_GATE,
    DEMO_WORKSPACE_OVERVIEW
}

internal fun decideDemoBackNavigation(
    requestedEntryIsDemo: Boolean,
    hasDemoWorkspaceSession: Boolean,
    hasSelectedDemoPersona: Boolean,
    hasSessionMemberId: Boolean
): DemoBackNavigationTarget? = when {
    hasDemoWorkspaceSession && !hasSessionMemberId -> DemoBackNavigationTarget.ENTRY_GATE
    requestedEntryIsDemo &&
        hasDemoWorkspaceSession &&
        hasSelectedDemoPersona &&
        hasSessionMemberId -> DemoBackNavigationTarget.DEMO_WORKSPACE_OVERVIEW
    else -> null
}
