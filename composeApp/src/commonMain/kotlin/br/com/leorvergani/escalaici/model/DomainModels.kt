package br.com.leorvergani.escalaici.model

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

/**
 * Portado 1:1 de `ShiftSwapStatus` (app real, `data/swap/`) — 6 estados
 * textuais distintos usados pela tela de Trocas de escala (FASE 10.11).
 */
enum class SwapStatus {
    PENDENTE_TECNICO_DESTINO,
    AGUARDANDO_COORDENADOR,
    APROVADA,
    RECUSADA_TECNICO_DESTINO,
    RECUSADA_COORDENADOR,
    CANCELADA
}

enum class ScheduleSourceType {
    LOCAL_FILE,
    LOCAL_CACHE,
    FIREBASE,
    ONEDRIVE,
    DROPBOX,
    DEMO
}

data class SchedulePeriod(
    val id: String,
    val teamId: String,
    val startDate: String,
    val endDate: String,
    val source: ScheduleSourceType,
    val updatedAt: String,
    val publicationRevision: Int? = null
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
    val notes: String? = null,
    val publicationRevision: Int? = null
)

enum class TeamManagerRole {
    PRIMARY_MANAGER,
    PRIMARY_APPROVER,
    OTHER
}

data class TeamManagerPermissions(
    val viewTeamSchedule: Boolean = false,
    val viewTeamMembers: Boolean = false,
    val editTeamSchedule: Boolean = false,
    val approveScheduleChanges: Boolean = false,
    val publishSchedule: Boolean = false,
    val manageTeamAssignments: Boolean = false
)

data class TeamManagerAssignment(
    val id: String,
    val workspaceId: String,
    val publicationRevision: Int,
    val teamId: String,
    val memberId: String,
    val role: TeamManagerRole,
    val permissions: TeamManagerPermissions = TeamManagerPermissions(),
    val active: Boolean = true,
    val validFrom: String,
    val validTo: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
    val createdBy: String = ""
)

data class ScheduleChangeRequest(
    val id: String,
    val workspaceId: String,
    val publicationRevision: Int,
    val memberId: String,
    val teamId: String,
    val periodId: String,
    val assignmentId: String? = null,
    val status: String,
    val assignedManagerMemberId: String,
    val requestType: String,
    val reason: String,
    val createdAt: String,
    val resolvedAt: String? = null,
    val resolvedByMemberId: String? = null,
    val resolutionNote: String? = null
)

data class WorkspacePublicationPointer(
    val workspaceId: String,
    val activeRevision: Int,
    val status: String? = null,
    val workspaceType: String? = null,
    val scenarioId: String? = null,
    val seedVersion: Int? = null,
    val allowedDeveloperObjectIds: List<String> = emptyList()
)

data class OnCallGroup(
    val id: String,
    val teamId: String,
    val name: String,
    val active: Boolean
)

data class OnCallPeriod(
    val id: String,
    val teamId: String,
    val startDate: String,
    val endDate: String,
    val updatedAt: String,
    val groupId: String? = null
)

data class OnCallAssignment(
    val id: String,
    val periodId: String,
    val teamId: String,
    val memberId: String,
    val memberName: String,
    val date: String,
    val startDate: String = date,
    val endDate: String = date,
    val startTime: String,
    val endTime: String,
    val status: OnCallStatus,
    val notes: String? = null,
    val groupId: String? = null
) {
    fun durationMinutes(): Long? {
        fun timeMinutes(value: String): Int? {
            val parts = value.split(":")
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour * 60 + minute
        }
        val startDay = LabDate.parseIso(startDate)?.epochDay()?.toLong() ?: return null
        val endDay = LabDate.parseIso(endDate)?.epochDay()?.toLong() ?: return null
        val startMinute = timeMinutes(startTime) ?: return null
        val endMinute = timeMinutes(endTime) ?: return null
        return ((endDay - startDay) * 24L * 60L + endMinute - startMinute).takeIf { it > 0 }
    }
}

data class ShiftSwapRequest(
    val id: String,
    val requesterMemberId: String,
    val targetMemberId: String,
    val originalDate: String,
    val requestedDate: String,
    val status: SwapStatus,
    val notes: String? = null,
    val requesterName: String = requesterMemberId,
    val targetName: String = targetMemberId,
    val requesterShiftType: ShiftType? = null,
    val targetShiftType: ShiftType? = null,
    val teamName: String? = null,
    val createdAt: String = ""
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
