package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.Team
import kotlinx.serialization.Serializable

@Serializable
data class DemoFixturePackage(
    val schemaVersion: Int,
    val workspace: DemoFixtureWorkspace,
    val teams: List<DemoFixtureTeam>,
    val members: List<DemoFixtureMember>,
    val memberTeamMemberships: List<DemoFixtureMembership>,
    val teamManagerAssignments: List<DemoFixtureManagerAssignment>,
    val scheduleChangeRequests: List<DemoFixtureScheduleChangeRequest>,
    val schedulePeriods: List<DemoFixtureSchedulePeriod>,
    val scheduleAssignments: List<DemoFixtureScheduleAssignment>,
    val publicationRecords: List<DemoFixturePublicationRecord>
)

@Serializable
data class DemoFixtureWorkspace(
    val workspaceId: String,
    val workspaceType: String,
    val scenarioId: String,
    val seedVersion: Int,
    val publicationRevision: Int,
    val externalEffectsAllowed: Boolean,
    val notificationsEnabled: Boolean
)

@Serializable
data class DemoFixtureTeam(
    val id: String,
    val workspaceId: String,
    val name: String,
    val acronym: String,
    val active: Boolean,
    val schemaVersion: Int
)

@Serializable
data class DemoFixtureMember(
    val id: String,
    val workspaceId: String,
    val displayName: String,
    val corporateLogin: String,
    val emailNormalized: String,
    val active: Boolean,
    val schemaVersion: Int
)

@Serializable
data class DemoFixtureMembership(
    val id: String,
    val workspaceId: String,
    val memberId: String,
    val teamId: String,
    val startDate: String,
    val endDate: String?,
    val active: Boolean,
    val isPrimary: Boolean,
    val schemaVersion: Int
)

@Serializable
data class DemoFixtureManagerAssignment(
    val id: String,
    val workspaceId: String,
    val managerMemberId: String,
    val teamId: String,
    val role: String,
    val permissions: DemoFixturePermissions,
    val active: Boolean,
    val validFrom: String,
    val validTo: String?,
    val createdAt: String,
    val updatedAt: String,
    val createdBy: String,
    val schemaVersion: Int
)

@Serializable
data class DemoFixturePermissions(
    val viewTeamSchedule: Boolean,
    val viewTeamMembers: Boolean,
    val editTeamSchedule: Boolean,
    val approveScheduleChanges: Boolean,
    val publishSchedule: Boolean,
    val manageTeamAssignments: Boolean
)

@Serializable
data class DemoFixtureScheduleChangeRequest(
    val id: String,
    val workspaceId: String,
    val requesterMemberId: String,
    val requesterTeamId: String,
    val assignedManagerMemberId: String,
    val schedulePeriodId: String,
    val assignmentId: String?,
    val requestType: String,
    val status: String,
    val reason: String,
    val createdAt: String,
    val resolvedAt: String?,
    val resolvedByMemberId: String?,
    val resolutionNote: String?,
    val schemaVersion: Int
)

@Serializable
data class DemoFixtureSchedulePeriod(
    val id: String,
    val workspaceId: String,
    val teamId: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val active: Boolean,
    val publicationRevision: Int,
    val schemaVersion: Int
)

@Serializable
data class DemoFixtureScheduleAssignment(
    val id: String,
    val workspaceId: String,
    val periodId: String,
    val teamId: String,
    val memberId: String,
    val date: String,
    val assignmentType: String,
    val shiftName: String?,
    val startTime: String?,
    val endTime: String?,
    val schemaVersion: Int
)

@Serializable
data class DemoFixturePublicationRecord(
    val id: String,
    val workspaceId: String,
    val publicationRevision: Int,
    val publishedAt: String,
    val publishedByMode: String,
    val dryRun: Boolean,
    val countsCreated: Int,
    val countsUpdated: Int,
    val countsDeleted: Int,
    val source: String,
    val schemaVersion: Int
)

fun DemoFixturePackage.toMembers(): List<Member> = members.map { member ->
    Member(
        email = member.emailNormalized,
        scaleName = member.displayName,
        displayName = member.displayName,
        id = member.id,
        active = member.active,
        workspaceId = member.workspaceId
    )
}

fun DemoFixturePackage.toTeams(): List<Team> = teams.map { team ->
    Team(
        teamId = team.id,
        name = team.name,
        displayName = team.name,
        workspaceId = team.workspaceId
    )
}

fun DemoFixturePackage.toMemberships(): List<MemberTeamMembership> = memberTeamMemberships.map { membership ->
    MemberTeamMembership(
        id = membership.id,
        memberId = membership.memberId,
        teamId = membership.teamId,
        roleId = null,
        startDate = membership.startDate,
        endDate = membership.endDate,
        active = membership.active,
        isPrimary = membership.isPrimary,
        workspaceId = membership.workspaceId
    )
}

fun DemoFixturePackage.loginByMemberId(): Map<String, String> =
    members.associate { it.id to it.corporateLogin }
