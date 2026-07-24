package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.ChangeRequestStatus
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.PauseDurationMinutes
import br.com.leorvergani.escalaici.model.PausePresentation
import br.com.leorvergani.escalaici.model.ScheduleChangeRequest
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.ShiftOccurrence
import br.com.leorvergani.escalaici.model.parseNotificationMinute
import br.com.leorvergani.escalaici.model.pauseWindowFor
import br.com.leorvergani.escalaici.model.plusMinutes
import br.com.leorvergani.escalaici.model.statusTyped
import br.com.leorvergani.escalaici.model.timeLabel

fun effectivePause(shift: ShiftOccurrence?, settings: NotificationSettings): PausePresentation? {
    val window = pauseWindowFor(shift) ?: return null
    val customStart = if (settings.notifyPause) {
        parseNotificationMinute(settings.pauseCustomTime)?.let { minute ->
            LabDateTime(window.start.date, minute)
        }
    } else {
        null
    }
    val scheduledLabel = customStart
        ?.takeIf { it >= window.start && it <= window.end }
        ?.let { start -> "${start.timeLabel()}–${start.plusMinutes(PauseDurationMinutes).timeLabel()}" }

    return PausePresentation(
        scheduledLabel = scheduledLabel,
        windowStart = window.startLabel,
        windowEnd = window.endLabel
    )
}

fun colleaguesForShift(day: ShiftDay): List<String> = day.membersByShift[day.type] ?: day.teamMembers

/**
 * Solicitações de troca/alteração relevantes para este usuário (FASE 14J, spec 67
 * seção 5.3): as que ele criou (`memberId`) ou as que aguardam a ação dele como
 * responsável designado (`assignedManagerMemberId` - fotografia imutável de quem
 * era o aprovador no momento da criação, já suficiente para essa checagem sem
 * precisar resolver o catálogo de papéis/`TeamManagerRole`, hoje desconectado).
 */
fun changeRequestsRelevantTo(memberId: String, all: List<ScheduleChangeRequest>): List<ScheduleChangeRequest> =
    all.filter { it.memberId == memberId || it.assignedManagerMemberId == memberId }

/** Mesma relevância acima, restrita ao status PENDING (o que importa para o card/badge). */
fun pendingChangeRequestsRelevantTo(memberId: String, all: List<ScheduleChangeRequest>): List<ScheduleChangeRequest> =
    changeRequestsRelevantTo(memberId, all).filter { it.statusTyped == ChangeRequestStatus.PENDING }
