package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.identity.DemoPersona
import br.com.leorvergani.escalaici.identity.OrganizationResolutionResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileIdentityDecisionTest {
    @Test
    fun demoPersonaSessionDoesNotShowOfficialOrganizationResolutionBody() {
        val decision = decideProfileIdentityPresentation(
            selectedDemoPersona = demoPersona(),
            organizationResolutionResult = null
        )

        assertFalse(decision.showOrganizationResolutionBody)
        assertTrue(decision.showDemoPersonaResolutionSection)
    }

    @Test
    fun demoPersonaSessionExplainsThatOfficialWorkspaceLinkAbsenceIsNotAnError() {
        val decision = decideProfileIdentityPresentation(
            selectedDemoPersona = demoPersona(),
            organizationResolutionResult = OrganizationResolutionResult.MemberNotFound(
                searchedEmail = "admin@example.invalid",
                searchedLogin = "admin"
            )
        )

        assertFalse(decision.showOrganizationResolutionBody)
        assertEquals(DemoOfficialWorkspaceNotRequiredMessage, decision.demoModeExplanation)
    }

    @Test
    fun loginSessionPreservesOfficialOrganizationResolutionBody() {
        val decision = decideProfileIdentityPresentation(
            selectedDemoPersona = null,
            organizationResolutionResult = OrganizationResolutionResult.MemberNotFound(
                searchedEmail = "user@example.invalid",
                searchedLogin = "user"
            )
        )

        assertTrue(decision.showOrganizationResolutionBody)
        assertFalse(decision.showDemoPersonaResolutionSection)
        assertEquals(null, decision.demoModeExplanation)
    }

    private fun demoPersona() = DemoPersona(
        personaId = "persona-demo",
        memberId = "member-demo",
        displayName = "Pessoa Demo",
        fictitiousLogin = "pessoa.demo",
        fictitiousEmail = "pessoa.demo@example.invalid"
    )
}
