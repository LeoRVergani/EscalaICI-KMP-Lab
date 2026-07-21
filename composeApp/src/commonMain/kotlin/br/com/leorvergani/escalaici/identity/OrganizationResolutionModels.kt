package br.com.leorvergani.escalaici.identity

data class ResolvedTeamMembership(
    val teamId: String,
    val teamName: String,
    val roleDisplayName: String?,
    val isPrimary: Boolean
)

data class ResolvedOrganizationContext(
    val workspaceId: String,
    val identitySource: IdentitySource,
    val memberId: String,
    val memberDisplayName: String,
    val normalizedLogin: String?,
    val normalizedEmail: String?,
    val primaryTeamId: String?,
    val primaryTeamName: String?,
    val roleDisplayName: String?,
    val activeMemberships: List<ResolvedTeamMembership>,
    val dataSourceMessage: String? = null,
    val publicationRevision: Int? = null
)
