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

/**
 * Uma solicitação de troca/alteração de escala (FASE 14J, spec 67 seção 5) - lida
 * de `workspaces/{id}/revisions/{n}/schedule_change_requests` (contrato real do
 * Dashboard, `contracts/organization-approval-v1.schema.json`). `memberId`/
 * `teamId`/`periodId` são os nomes internos para `requesterMemberId`/
 * `requesterTeamId`/`schedulePeriodId` do Firestore (mesmo dado, alias já
 * resolvido na fronteira de parsing - ver `DemoPublicationDtos.toDemoChangeRequest`).
 * `status`/`requestType` continuam `String` livre (preserva o valor de origem,
 * mesmo espírito de `ShiftDay.sourceStatus`) - use [statusTyped]/[requestTypeTyped]
 * para leitura tipada, com fallback seguro para `UNKNOWN` em vez de lançar.
 */
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

/** Valores reais escritos pelo Dashboard (`DemoChangeRequestStatus`, `dto.ts`). */
enum class ChangeRequestStatus {
    DRAFT, PENDING, APPROVED, REJECTED, CANCELLED, EXPIRED, UNKNOWN;

    companion object {
        fun parse(raw: String): ChangeRequestStatus = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

/** Valores reais escritos pelo Dashboard (`DemoChangeRequestType`, `dto.ts`). */
enum class ChangeRequestType {
    SHIFT_CHANGE, DAY_OFF_CHANGE, SWAP_WITH_MEMBER, SCHEDULE_CORRECTION, OTHER, UNKNOWN;

    companion object {
        fun parse(raw: String): ChangeRequestType = entries.firstOrNull { it.name == raw } ?: UNKNOWN
    }
}

val ScheduleChangeRequest.statusTyped: ChangeRequestStatus get() = ChangeRequestStatus.parse(status)
val ScheduleChangeRequest.requestTypeTyped: ChangeRequestType get() = ChangeRequestType.parse(requestType)

/** Rótulos idênticos aos do Dashboard (`STATUS_LABELS`, `DemoChangeRequestsDialog.tsx`). */
fun ChangeRequestStatus.label(): String = when (this) {
    ChangeRequestStatus.DRAFT -> "Rascunho"
    ChangeRequestStatus.PENDING -> "Pendente"
    ChangeRequestStatus.APPROVED -> "Aprovada"
    ChangeRequestStatus.REJECTED -> "Recusada"
    ChangeRequestStatus.CANCELLED -> "Cancelada"
    ChangeRequestStatus.EXPIRED -> "Expirada"
    ChangeRequestStatus.UNKNOWN -> "Desconhecido"
}

/** Rótulos idênticos aos do Dashboard (`REQUEST_TYPE_LABELS`, `DemoChangeRequestsDialog.tsx`). */
fun ChangeRequestType.label(): String = when (this) {
    ChangeRequestType.SHIFT_CHANGE -> "Troca de turno"
    ChangeRequestType.DAY_OFF_CHANGE -> "Troca de folga"
    ChangeRequestType.SWAP_WITH_MEMBER -> "Troca com colega"
    ChangeRequestType.SCHEDULE_CORRECTION -> "Correção de escala"
    ChangeRequestType.OTHER -> "Outro"
    ChangeRequestType.UNKNOWN -> "Desconhecido"
}

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
