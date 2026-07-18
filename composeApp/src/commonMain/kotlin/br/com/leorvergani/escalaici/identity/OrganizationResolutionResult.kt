package br.com.leorvergani.escalaici.identity

sealed interface OrganizationResolutionResult {
    data class Resolved(val context: ResolvedOrganizationContext) : OrganizationResolutionResult
    data class MemberFoundNoActiveTeam(val context: ResolvedOrganizationContext) : OrganizationResolutionResult
    data class MemberNotFound(val searchedEmail: String?, val searchedLogin: String?) : OrganizationResolutionResult
    data class MemberInactive(val memberId: String) : OrganizationResolutionResult
    data class MemberIdentityAmbiguous(val candidateMemberIds: List<String>) : OrganizationResolutionResult
    data class MembershipNotFound(val memberId: String) : OrganizationResolutionResult
    data class TeamNotFound(val memberId: String, val teamId: String) : OrganizationResolutionResult
    data class MultipleActiveTeams(val memberId: String, val teamIds: List<String>) : OrganizationResolutionResult
    data class WorkspaceMismatch(val expectedWorkspaceId: String, val actualWorkspaceId: String) : OrganizationResolutionResult
    data class DataSourceUnavailable(val message: String) : OrganizationResolutionResult
}

enum class OrganizationResolutionStatus {
    IDLE,
    RESOLVING,
    MEMBER_FOUND,
    MEMBER_NOT_FOUND,
    TEAM_FOUND,
    TEAM_NOT_FOUND,
    MULTIPLE_ACTIVE_TEAMS,
    ERROR
}

fun OrganizationResolutionResult.toStatus(): OrganizationResolutionStatus = when (this) {
    is OrganizationResolutionResult.Resolved -> OrganizationResolutionStatus.TEAM_FOUND
    is OrganizationResolutionResult.MemberFoundNoActiveTeam -> OrganizationResolutionStatus.MEMBER_FOUND
    is OrganizationResolutionResult.MemberNotFound,
    is OrganizationResolutionResult.MemberInactive,
    is OrganizationResolutionResult.MemberIdentityAmbiguous -> OrganizationResolutionStatus.MEMBER_NOT_FOUND
    is OrganizationResolutionResult.TeamNotFound -> OrganizationResolutionStatus.TEAM_NOT_FOUND
    is OrganizationResolutionResult.MultipleActiveTeams -> OrganizationResolutionStatus.MULTIPLE_ACTIVE_TEAMS
    // Contract/data-shape failures are not recoverable as a normal member/team lookup state.
    is OrganizationResolutionResult.DataSourceUnavailable,
    is OrganizationResolutionResult.WorkspaceMismatch,
    is OrganizationResolutionResult.MembershipNotFound -> OrganizationResolutionStatus.ERROR
}
