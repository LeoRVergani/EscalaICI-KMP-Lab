package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.PauseDurationMinutes
import br.com.leorvergani.escalaici.model.PausePresentation
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.ShiftOccurrence
import br.com.leorvergani.escalaici.model.parseNotificationMinute
import br.com.leorvergani.escalaici.model.pauseWindowFor
import br.com.leorvergani.escalaici.model.plusMinutes
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
