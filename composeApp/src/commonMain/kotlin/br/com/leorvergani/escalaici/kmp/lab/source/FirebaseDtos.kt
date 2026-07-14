package br.com.leorvergani.escalaici.kmp.lab.source

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseTeamDto(
    val teamId: String,
    val teamName: String,
    val active: Boolean
)

@Serializable
data class FirebaseMemberDto(
    val memberId: String,
    val teamId: String,
    val displayName: String,
    val scaleName: String,
    val title: String? = null,
    val role: String? = null,
    val active: Boolean
)

@Serializable
data class FirebaseSchedulePeriodDto(
    val periodId: String,
    val teamId: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val active: Boolean,
    val updatedAt: String
)

@Serializable
data class FirebaseScheduleAssignmentDto(
    val assignmentId: String,
    val teamId: String,
    val periodId: String,
    val memberId: String?,
    val scaleName: String,
    val date: String,
    val assignmentType: String,
    val shiftName: String? = null,
    val startDateTime: String? = null,
    val endDateTime: String? = null,
    val note: String? = null
)

@Serializable
data class FirebaseOnCallPeriodDto(
    val periodId: String,
    val teamId: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val active: Boolean,
    val updatedAt: String
)

@Serializable
data class FirebaseOnCallAssignmentDto(
    val onCallId: String,
    val teamId: String,
    val periodId: String?,
    val memberId: String?,
    val scaleName: String,
    val startDateTime: String,
    val endDateTime: String,
    val label: String,
    val active: Boolean,
    val notes: String? = null
)

@Serializable
data class FirebaseScheduleSnapshot(
    val team: FirebaseTeamDto,
    val period: FirebaseSchedulePeriodDto,
    val members: List<FirebaseMemberDto>,
    val assignments: List<FirebaseScheduleAssignmentDto>,
    val cachedAt: String,
    val lastAttemptAt: String
)

@Serializable
data class FirebaseOnCallSnapshot(
    val period: FirebaseOnCallPeriodDto,
    val members: List<FirebaseMemberDto>,
    val assignments: List<FirebaseOnCallAssignmentDto>,
    val cachedAt: String,
    val lastAttemptAt: String
)
