package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.identity.DEMO_DEVELOPER_ROLE
import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult

internal const val DemoOfficialWorkspaceNotRequiredMessage =
    "Este modo não depende de vínculo no workspace oficial; os dados abaixo são da persona fictícia selecionada."

internal data class ProfileIdentityDecision(
    val showOrganizationResolutionBody: Boolean,
    val showDemoPersonaResolutionSection: Boolean,
    val demoSessionLabel: String?,
    val demoModeExplanation: String?
)

@Suppress("UNUSED_PARAMETER")
internal fun decideProfileIdentityPresentation(
    selectedDemoPersona: DemoPersona?,
    organizationResolutionResult: OrganizationResolutionResult?
): ProfileIdentityDecision {
    val isDemoPersonaSession = selectedDemoPersona != null
    return if (isDemoPersonaSession) {
        ProfileIdentityDecision(
            showOrganizationResolutionBody = false,
            showDemoPersonaResolutionSection = true,
            demoSessionLabel = "Sessão administrativa Demo ($DEMO_DEVELOPER_ROLE)",
            demoModeExplanation = DemoOfficialWorkspaceNotRequiredMessage
        )
    } else {
        ProfileIdentityDecision(
            showOrganizationResolutionBody = true,
            showDemoPersonaResolutionSection = false,
            demoSessionLabel = null,
            demoModeExplanation = null
        )
    }
}
