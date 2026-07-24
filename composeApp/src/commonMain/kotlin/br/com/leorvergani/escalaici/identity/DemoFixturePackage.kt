package br.com.leorvergani.escalaici.identity

import br.com.leorvergani.escalaici.model.Member
import br.com.leorvergani.escalaici.model.MemberTeamMembership
import br.com.leorvergani.escalaici.model.AssignmentSource
import br.com.leorvergani.escalaici.model.ScheduleAssignment
import br.com.leorvergani.escalaici.model.ScheduleChangeRequest
import br.com.leorvergani.escalaici.model.SchedulePeriod
import br.com.leorvergani.escalaici.model.ScheduleSourceType
import br.com.leorvergani.escalaici.model.ShiftType
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
        workspaceId = member.workspaceId,
        publicationRevision = workspace.publicationRevision
    )
}

fun DemoFixturePackage.toTeams(): List<Team> = teams.map { team ->
    Team(
        teamId = team.id,
        name = team.name,
        displayName = team.name,
        workspaceId = team.workspaceId,
        publicationRevision = workspace.publicationRevision
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
        workspaceId = membership.workspaceId,
        publicationRevision = workspace.publicationRevision
    )
}

fun DemoFixturePackage.toSchedulePeriods(): List<SchedulePeriod> = schedulePeriods.map { period ->
    SchedulePeriod(
        id = period.id,
        teamId = period.teamId,
        startDate = period.startDate,
        endDate = period.endDate,
        source = ScheduleSourceType.DEMO,
        updatedAt = "",
        publicationRevision = period.publicationRevision
    )
}

fun DemoFixturePackage.toScheduleAssignments(): List<ScheduleAssignment> = scheduleAssignments.map { assignment ->
    ScheduleAssignment(
        id = assignment.id,
        periodId = assignment.periodId,
        teamId = assignment.teamId,
        memberId = assignment.memberId,
        memberName = members.firstOrNull { it.id == assignment.memberId }?.displayName ?: assignment.memberId,
        date = assignment.date,
        shiftType = fixtureShiftType(assignment.assignmentType, assignment.shiftName),
        startTime = assignment.startTime,
        endTime = assignment.endTime,
        source = AssignmentSource.FIREBASE_SYNC,
        publicationRevision = workspace.publicationRevision
    )
}

fun DemoFixturePackage.loginByMemberId(): Map<String, String> =
    members.associate { it.id to it.corporateLogin }

fun DemoFixturePackage.toScheduleChangeRequests(): List<ScheduleChangeRequest> = scheduleChangeRequests.map { request ->
    ScheduleChangeRequest(
        id = request.id,
        workspaceId = request.workspaceId,
        publicationRevision = workspace.publicationRevision,
        memberId = request.requesterMemberId,
        teamId = request.requesterTeamId,
        periodId = request.schedulePeriodId,
        assignmentId = request.assignmentId,
        status = request.status,
        assignedManagerMemberId = request.assignedManagerMemberId,
        requestType = request.requestType,
        reason = request.reason,
        createdAt = request.createdAt,
        resolvedAt = request.resolvedAt,
        resolvedByMemberId = request.resolvedByMemberId,
        resolutionNote = request.resolutionNote
    )
}

private fun fixtureShiftType(assignmentType: String, shiftName: String?): ShiftType = when (assignmentType) {
    "OFF" -> ShiftType.FOLGA
    "VACATION" -> ShiftType.FERIAS
    "WORK_SHIFT" -> when (shiftName?.lowercase()) {
        "madrugada" -> ShiftType.MADRUGADA
        "manhã", "manha", "morning" -> ShiftType.MANHA
        "tarde", "afternoon" -> ShiftType.TARDE
        "noite", "night" -> ShiftType.NOITE
        "comercial" -> ShiftType.COMERCIAL
        else -> ShiftType.INDEFINIDO
    }
    else -> ShiftType.INDEFINIDO
}
