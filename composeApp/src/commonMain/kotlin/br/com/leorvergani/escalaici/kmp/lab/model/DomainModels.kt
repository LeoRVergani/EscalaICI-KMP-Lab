package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Modelos puros de dominio da FASE 9c, extraidos da spec
 * `docs/spec/27-KMP-PWA-IOS-ESTRATEGIA.md` (repositorio Android principal).
 *
 * Kotlin puro, sem dependencia de Android/JVM/Firebase/MSAL. Datas e horarios
 * ficam como `String` ate o laboratorio definir uma estrategia multiplataforma
 * de data/hora.
 */

enum class MemberRole {
    ANALYST,
    LEAD,
    ADMIN
}

enum class AssignmentSource {
    MANUAL,
    IMPORTED_XLS,
    FIREBASE_SYNC
}

enum class ImportStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED
}

enum class OnCallStatus {
    SCHEDULED,
    ACTIVE,
    COMPLETED,
    CANCELLED
}

enum class SwapStatus {
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED
}

enum class ScheduleSourceType {
    MOCK,
    IMPORTED_XLS,
    FIREBASE
}

data class SchedulePeriod(
    val id: String,
    val teamId: String,
    val startDate: String,
    val endDate: String,
    val source: ScheduleSourceType,
    val updatedAt: String
)

data class ScheduleAssignment(
    val id: String,
    val periodId: String,
    val teamId: String,
    val memberId: String,
    val memberName: String,
    val date: String,
    val shiftType: ShiftType,
    val startTime: String? = null,
    val endTime: String? = null,
    val source: AssignmentSource = AssignmentSource.MANUAL,
    val notes: String? = null
)

data class OnCallPeriod(
    val id: String,
    val teamId: String,
    val startDate: String,
    val endDate: String,
    val updatedAt: String
)

data class OnCallAssignment(
    val id: String,
    val periodId: String,
    val teamId: String,
    val memberId: String,
    val memberName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val status: OnCallStatus,
    val notes: String? = null
)

data class ShiftSwapRequest(
    val id: String,
    val requesterMemberId: String,
    val targetMemberId: String,
    val originalDate: String,
    val requestedDate: String,
    val status: SwapStatus,
    val notes: String? = null
)

data class ImportJob(
    val id: String,
    val sourceFileId: String,
    val status: ImportStatus,
    val createdAt: String,
    val finishedAt: String? = null,
    val message: String? = null
)

data class SourceFileRecord(
    val id: String,
    val fileName: String,
    val sourceType: ScheduleSourceType,
    val createdAt: String,
    val updatedAt: String,
    val hash: String? = null
)
