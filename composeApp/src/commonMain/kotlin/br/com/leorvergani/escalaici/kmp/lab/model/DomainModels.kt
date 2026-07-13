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
    val startDate: String = date,
    val endDate: String = date,
    val startTime: String,
    val endTime: String,
    val status: OnCallStatus,
    val notes: String? = null
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
