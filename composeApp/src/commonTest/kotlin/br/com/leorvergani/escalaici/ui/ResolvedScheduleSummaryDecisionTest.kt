package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.identity.IdentitySource
import br.com.leorvergani.escalaici.identity.ResolvedOrganizationContext
import br.com.leorvergani.escalaici.identity.ResolvedTeamMembership
import br.com.leorvergani.escalaici.model.mockScheduleSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ResolvedScheduleSummaryDecisionTest {
    @Test
    fun resolvedDemoPersonaWithMissingPublishedSummaryReturnsErrorWithoutMock() {
        val decision = decideResolvedScheduleSummary(
            currentSummary = mockScheduleSummary(),
            resolvedContext = resolvedOperationalContext(),
            loadPublishedScheduleSummaryAvailable = true,
            publishedSummary = null
        )

        assertNull(decision.summary)
        assertEquals(ActiveScheduleMissingMessage, decision.errorMessage)
    }

    @Test
    fun resolvedDemoPersonaWithoutPublishedSummaryProviderKeepsMetadataFallback() {
        val decision = decideResolvedScheduleSummary(
            currentSummary = mockScheduleSummary(),
            resolvedContext = resolvedOperationalContext(),
            loadPublishedScheduleSummaryAvailable = false,
            publishedSummary = null
        )

        val summary = assertNotNull(decision.summary)
        assertNull(decision.errorMessage)
        assertEquals("member-demo-soc-01", summary.member.id)
        assertEquals("Analista SOC Demo 1", summary.member.displayName)
        assertEquals("team-demo-soc", summary.team.teamId)
    }

    private fun resolvedOperationalContext() = ResolvedOrganizationContext(
        workspaceId = "demo-v1",
        identitySource = IdentitySource.DEMO_PERSONA,
        memberId = "member-demo-soc-01",
        memberDisplayName = "Analista SOC Demo 1",
        normalizedLogin = "analista.soc.demo1",
        normalizedEmail = "analista.soc.demo1@example.invalid",
        primaryTeamId = "team-demo-soc",
        primaryTeamName = "SOC Demo",
        roleDisplayName = "Analista",
        activeMemberships = listOf(
            ResolvedTeamMembership(
                teamId = "team-demo-soc",
                teamName = "SOC Demo",
                roleDisplayName = "Analista",
                isPrimary = true
            )
        )
    )
}
