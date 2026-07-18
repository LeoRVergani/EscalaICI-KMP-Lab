package br.com.leorvergani.escalaici.identity

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class DemoFixtureRealFileTest {
    @Test
    fun realPublicationPackageParsesWithKotlinDtos() {
        val file = existingFixtureFile()
        val pkg = parseDemoFixturePackage(file.readText())

        assertEquals("demo-v1", pkg.workspace.workspaceId)
        assertEquals(5, pkg.members.size)
        assertEquals(2, pkg.teams.size)
        assertEquals(124, pkg.scheduleAssignments.size)

        val managerMemberships = pkg.memberTeamMemberships.filter {
            it.memberId == "member-demo-gestor-seguranca" && it.active
        }
        assertEquals(1, managerMemberships.size)
        assertEquals("team-demo-seguranca", managerMemberships.single().teamId)

        val managerAssignments = pkg.teamManagerAssignments.filter {
            it.managerMemberId == "member-demo-gestor-seguranca"
        }
        assertEquals(2, managerAssignments.size)
    }

    private fun existingFixtureFile(): File {
        val candidates = listOf(
            // Android unit tests can run with either the repo root or composeApp as working directory.
            File("src/commonMain/composeResources/files/demo/demo-v1-publication-package.json"),
            File("composeApp/src/commonMain/composeResources/files/demo/demo-v1-publication-package.json")
        )
        return candidates.first { it.exists() }
    }
}
