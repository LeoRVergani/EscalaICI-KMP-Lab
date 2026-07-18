package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.LabDate

data class OrganizationIdentityCacheEntry(
    val workspaceId: String,
    val identitySource: IdentitySource,
    val memberId: String,
    val primaryTeamId: String?,
    val dataUpdatedAt: LabDate
) {
    val key: String get() = organizationIdentityCacheKey(workspaceId, memberId = memberId)
}

fun organizationIdentityCacheKey(
    workspaceId: String,
    normalizedEmail: String? = null,
    normalizedLogin: String? = null,
    memberId: String
): String = "$workspaceId:${normalizedEmail ?: normalizedLogin ?: memberId}"

interface OrganizationIdentityCacheStorage {
    suspend fun save(entry: OrganizationIdentityCacheEntry)
    suspend fun load(key: String): OrganizationIdentityCacheEntry?
    suspend fun clear(key: String)
}

class InMemoryOrganizationIdentityCacheStorage : OrganizationIdentityCacheStorage {
    private val entries = mutableMapOf<String, OrganizationIdentityCacheEntry>()

    override suspend fun save(entry: OrganizationIdentityCacheEntry) {
        entries[entry.key] = entry
    }

    override suspend fun load(key: String): OrganizationIdentityCacheEntry? = entries[key]

    override suspend fun clear(key: String) {
        entries.remove(key)
    }
}
