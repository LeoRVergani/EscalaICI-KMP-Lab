package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.source.DemoPublicationGateway
import br.com.leorvergani.escalaici.source.DemoPublicationResolver
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

class RemoteFirstDemoMemberDirectoryRepositoryTest {
    @Test
    fun remoteFirstDirectoryPassesEntraIdentityParametersToInMemoryDirectory() = runTest {
        val publicationRepository = DemoPublicationRepository(
            resolver = DemoPublicationResolver(
                gateway = FakeDemoPublicationGateway(
                    workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
                    members = listOf(
                        member(
                            id = "member-email",
                            workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
                            email = "same.email@example.invalid",
                            displayName = "Same Email"
                        ),
                        member(
                            id = "member-entra",
                            workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID,
                            email = "other.email@example.invalid",
                            displayName = "Other Email",
                            entraTenantId = "tenant-1",
                            entraObjectId = "object-1"
                        )
                    )
                ),
                workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID
            ),
            fixtureProvider = null
        )

        val result = RemoteFirstDemoMemberDirectoryRepository(
            publicationRepository = publicationRepository,
            workspaceId = OrganizationWorkspace.CORPORATE_WORKSPACE_ID
        ).findActiveMemberIds(
            normalizedEmail = "same.email@example.invalid",
            normalizedLogin = null,
            entraTenantId = "tenant-1",
            entraObjectId = "object-1"
        )

        assertEquals(listOf("member-entra"), result)
    }
}

private class FakeDemoPublicationGateway(
    private val workspaceId: String,
    private val members: List<JsonObject>,
    private val revision: Int = 7
) : DemoPublicationGateway {
    override suspend fun loadDocumentFields(path: String): JsonObject =
        workspacePointer(workspaceId, revision)

    override suspend fun loadCollectionDocuments(path: String): List<JsonObject> = when (path.substringAfterLast("/")) {
        "members" -> members
        "teams",
        "member_team_memberships",
        "team_manager_assignments",
        "schedule_periods",
        "schedule_assignments",
        "schedule_change_requests" -> emptyList()
        else -> error("Unexpected collection path: $path")
    }
}

private fun workspacePointer(workspaceId: String, revision: Int) = buildJsonObject {
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("status", "ACTIVE")
}

private fun member(
    id: String,
    workspaceId: String,
    email: String,
    displayName: String,
    entraTenantId: String? = null,
    entraObjectId: String? = null,
    revision: Int = 7
) = buildJsonObject {
    putField("id", id)
    putField("workspaceId", workspaceId)
    putField("publicationRevision", revision)
    putField("displayName", displayName)
    putField("emailNormalized", email)
    putBoolField("active", true)
    entraTenantId?.let { putField("entraTenantId", it) }
    entraObjectId?.let { putField("entraObjectId", it) }
}

private fun JsonObjectBuilder.putField(name: String, value: String) {
    put(name, buildJsonObject { put("stringValue", value) })
}

private fun JsonObjectBuilder.putField(name: String, value: Int) {
    put(name, buildJsonObject { put("integerValue", value.toString()) })
}

private fun JsonObjectBuilder.putBoolField(name: String, value: Boolean) {
    put(name, buildJsonObject { put("booleanValue", value) })
}
