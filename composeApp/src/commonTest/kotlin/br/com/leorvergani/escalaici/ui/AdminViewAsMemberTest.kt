package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.Member
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdminViewAsMemberTest {
    private fun member(
        id: String,
        displayName: String,
        active: Boolean = true,
        corporateLogin: String? = null,
        teamId: String = "team-soc"
    ) = Member(
        email = "$id@example.invalid",
        scaleName = displayName,
        displayName = displayName,
        id = id,
        teamId = teamId,
        active = active,
        corporateLogin = corporateLogin
    )

    // 1/2. autorizacao reaproveita demoAccessGranted, sem comparacao de e-mail nova.
    @Test
    fun authorizedAdministratorSeesTheFunction() {
        assertTrue(isViewAsMemberAuthorized(administrativeAccessGranted = true))
    }

    @Test
    fun regularUserDoesNotSeeTheFunction() {
        assertFalse(isViewAsMemberAuthorized(administrativeAccessGranted = false))
    }

    // 3/4. lista contem somente membros ativos da equipe atual; membro inativo nao aparece.
    @Test
    fun collaboratorListContainsOnlyActiveMembersOfCurrentTeam() {
        val members = listOf(
            member("member-a", "Ana"),
            member("member-b", "Bruno", active = false),
            member("member-c", "Carla")
        )

        val options = collaboratorOptionsForViewAs(
            members = members,
            teamId = "team-soc",
            teamName = "SOC",
            excludeMemberId = "member-admin"
        )

        assertEquals(listOf("member-a", "member-c"), options.map { it.memberId })
    }

    @Test
    fun collaboratorListNeverIncludesTheAuthenticatedAdministrator() {
        val members = listOf(member("member-admin", "Leonardo"), member("member-a", "Ana"))

        val options = collaboratorOptionsForViewAs(
            members = members,
            teamId = "team-soc",
            teamName = "SOC",
            excludeMemberId = "member-admin"
        )

        assertEquals(listOf("member-a"), options.map { it.memberId })
    }

    @Test
    fun collaboratorListIsSortedAlphabetically() {
        val members = listOf(member("member-c", "Carla"), member("member-a", "Ana"), member("member-b", "Bruno"))

        val options = collaboratorOptionsForViewAs(
            members = members,
            teamId = "team-soc",
            teamName = "SOC",
            excludeMemberId = "member-admin"
        )

        assertEquals(listOf("Ana", "Bruno", "Carla"), options.map { it.displayName })
    }

    @Test
    fun searchMatchesNameLoginOrTeam() {
        val options = listOf(
            ViewableCollaborator("member-a", "Ana Paula", "ana.paula", "team-soc", "SOC"),
            ViewableCollaborator("member-b", "Bruno Costa", "bruno.costa", "team-noc", "NOC")
        )

        assertEquals(listOf("member-a"), filterViewAsCollaborators(options, "ana").map { it.memberId })
        assertEquals(listOf("member-b"), filterViewAsCollaborators(options, "bruno.costa").map { it.memberId })
        assertEquals(listOf("member-b"), filterViewAsCollaborators(options, "noc").map { it.memberId })
        assertEquals(2, filterViewAsCollaborators(options, "").size)
        assertEquals(0, filterViewAsCollaborators(options, "inexistente").size)
    }

    // Modelo de identidade: authenticatedMemberId nunca muda; effectiveMemberId so para apresentacao.
    @Test
    fun effectiveMemberIdFallsBackToAuthenticatedWhenNotViewing() {
        val context = ViewAsIdentityContext(authenticatedMemberId = "member-admin", viewedMemberId = null)

        assertEquals("member-admin", context.effectiveMemberId)
        assertFalse(context.isViewingOther)
    }

    @Test
    fun effectiveMemberIdIsTheViewedMemberWhenViewing() {
        val context = ViewAsIdentityContext(authenticatedMemberId = "member-admin", viewedMemberId = "member-b")

        assertEquals("member-b", context.effectiveMemberId)
        assertTrue(context.isViewingOther)
    }

    // 16. troca de revisao/workspace/equipe limpa a persona visualizada.
    @Test
    fun publicationRevisionChangeClearsViewAs() {
        val previous = ViewAsPublicationContext("ici-dev", "team-soc", 2)
        val current = previous.copy(publicationRevision = 3)

        assertTrue(shouldClearViewAsOnPublicationChange("member-b", previous, current))
    }

    @Test
    fun workspaceChangeClearsViewAs() {
        val previous = ViewAsPublicationContext("ici-dev", "team-soc", 2)
        val current = previous.copy(workspaceId = "demo-v1")

        assertTrue(shouldClearViewAsOnPublicationChange("member-b", previous, current))
    }

    @Test
    fun teamChangeClearsViewAs() {
        val previous = ViewAsPublicationContext("ici-dev", "team-soc", 2)
        val current = previous.copy(teamId = "team-noc")

        assertTrue(shouldClearViewAsOnPublicationChange("member-b", previous, current))
    }

    @Test
    fun sameContextDoesNotClearViewAs() {
        val previous = ViewAsPublicationContext("ici-dev", "team-soc", 2)

        assertFalse(shouldClearViewAsOnPublicationChange("member-b", previous, previous))
    }

    @Test
    fun noPersonaSelectedNeverTriggersClear() {
        val previous = ViewAsPublicationContext("ici-dev", "team-soc", 2)
        val current = previous.copy(publicationRevision = 3)

        assertFalse(shouldClearViewAsOnPublicationChange(null, previous, current))
    }
}
