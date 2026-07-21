package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.LabDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

class OrganizationIdentityCacheTest {

    @Test
    fun cacheSavesAndLoadsByWorkspaceIdentityKey() = runTest {
        val storage = InMemoryOrganizationIdentityCacheStorage()
        val entry = entry(memberId = "member-1")
        val key = organizationIdentityCacheKey(workspaceId = "ici-dev", memberId = "member-1")

        storage.save(entry)

        assertEquals(entry, storage.load(key))
    }

    @Test
    fun cacheKeyFromOtherWorkspaceDoesNotInterfere() = runTest {
        val storage = InMemoryOrganizationIdentityCacheStorage()
        val entry = entry(workspaceId = "ici-dev", memberId = "member-1")

        storage.save(entry)

        assertNull(storage.load(organizationIdentityCacheKey(workspaceId = "demo-v1", memberId = "member-1")))
    }

    @Test
    fun clearingOneKeyDoesNotRemoveOtherWorkspace() = runTest {
        val storage = InMemoryOrganizationIdentityCacheStorage()
        val corporate = entry(workspaceId = "ici-dev", memberId = "member-1")
        val demo = entry(workspaceId = "demo-v1", memberId = "member-1", source = IdentitySource.DEMO_PERSONA)

        storage.save(corporate)
        storage.save(demo)
        storage.clear(organizationIdentityCacheKey(workspaceId = "ici-dev", memberId = "member-1"))

        assertNull(storage.load(organizationIdentityCacheKey(workspaceId = "ici-dev", memberId = "member-1")))
        assertEquals(demo, storage.load(organizationIdentityCacheKey(workspaceId = "demo-v1", memberId = "member-1")))
    }

    private fun entry(
        workspaceId: String = "ici-dev",
        memberId: String,
        source: IdentitySource = IdentitySource.CORPORATE_MSAL
    ) = OrganizationIdentityCacheEntry(
        workspaceId = workspaceId,
        identitySource = source,
        memberId = memberId,
        primaryTeamId = "team-1",
        dataUpdatedAt = LabDate(2026, 7, 18)
    )
}
