package br.com.leorvergani.escalaici.model

data class NotificationSettings(
    val notifyDayBefore: Boolean = true,
    val dayBeforeTime: String = "18:00",
    val notifyShiftStart: Boolean = true,
    val shiftStartOffsetMinutes: Int = 30,
    val notifyShiftEnd: Boolean = true,
    val notifyPause: Boolean = true,
    val pauseCustomTime: String? = null,
    val notifyScheduleChanged: Boolean = true,
    val lastRescheduleAt: String? = null
)

enum class NotificationType {
    DAY_BEFORE_WORK,
    DAY_BEFORE_REST,
    SHIFT_START,
    SHIFT_END,
    PAUSE_START,
    PAUSE_END
}

data class ScheduledNotification(
    val id: String,
    val type: NotificationType,
    val triggerAt: LabDateTime,
    val title: String,
    val body: String
)

val ShiftStartOffsetOptions = listOf(0, 5, 10, 15, 30, 60)
const val PauseDurationMinutes = 15

fun buildNotificationPlan(
    days: List<ShiftDay>,
    settings: NotificationSettings,
    now: LabDateTime
): List<ScheduledNotification> {
    val dayBeforeMinute = parseNotificationMinute(settings.dayBeforeTime)
    val shiftStartOffset = settings.shiftStartOffsetMinutes.takeIf { it in ShiftStartOffsetOptions } ?: 30

    return days
        .filter { day -> day.date?.let { it >= now.date } == true }
        .flatMap { day ->
            val date = day.date ?: return@flatMap emptyList()
            val events = mutableListOf<ScheduledNotification>()
            val occurrence = day.shiftOccurrence()

            if (settings.notifyDayBefore && day.type != ShiftType.INDEFINIDO && dayBeforeMinute != null) {
                val type = if (day.type.isWorkShift) NotificationType.DAY_BEFORE_WORK else NotificationType.DAY_BEFORE_REST
                val trigger = LabDateTime(date.plusDays(-1), dayBeforeMinute)
                events += scheduled(
                    date = date,
                    type = type,
                    triggerAt = trigger,
                    title = if (day.type.isWorkShift) "Você trabalha amanhã" else "Você está de folga amanhã",
                    body = if (day.type.isWorkShift) "${day.type.label}: ${day.type.timeRange}" else day.label
                )
            }

            if (occurrence != null && settings.notifyShiftStart) {
                events += scheduled(
                    date = date,
                    type = NotificationType.SHIFT_START,
                    triggerAt = occurrence.start.plusMinutes(-shiftStartOffset),
                    title = "Entrada do turno",
                    body = if (shiftStartOffset == 0) {
                        "Seu turno ${day.type.label} começa agora."
                    } else {
                        "Seu turno ${day.type.label} começa em $shiftStartOffset min."
                    }
                )
            }

            if (occurrence != null && settings.notifyShiftEnd) {
                events += scheduled(
                    date = date,
                    type = NotificationType.SHIFT_END,
                    triggerAt = occurrence.end,
                    title = "Fim do turno",
                    body = "Seu turno ${day.type.label} termina agora."
                )
            }

            if (occurrence != null && settings.notifyPause) {
                pauseStartFor(occurrence, settings.pauseCustomTime)?.let { pauseStart ->
                    events += scheduled(
                        date = date,
                        type = NotificationType.PAUSE_START,
                        triggerAt = pauseStart,
                        title = "Pausa de 15 minutos",
                        body = "Hora de iniciar sua pausa."
                    )
                    events += scheduled(
                        date = date,
                        type = NotificationType.PAUSE_END,
                        triggerAt = pauseStart.plusMinutes(PauseDurationMinutes),
                        title = "Fim da pausa",
                        body = "Sua pausa de 15 minutos terminou."
                    )
                }
            }

            events
        }
        .filter { it.triggerAt >= now }
        .sortedWith(compareBy<ScheduledNotification> { it.triggerAt }.thenBy { it.type.ordinal }.thenBy { it.id })
}

fun pauseSuggestionTimes(shift: ShiftOccurrence?): List<String> {
    val window = pauseWindowFor(shift) ?: return emptyList()
    val result = mutableListOf<String>()
    var candidate = window.start
    while (candidate <= window.end) {
        result += candidate.timeLabel()
        candidate = candidate.plusMinutes(30)
    }
    return result
}

fun parseNotificationMinute(value: String?): Int? {
    val parts = value?.split(":") ?: return null
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return (hour * 60 + minute).takeIf { hour in 0..23 && minute in 0..59 }
}

fun LabDate.isoLabel(): String =
    "${year.toString().padStart(4, '0')}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

fun LabDateTime.isoMinuteLabel(): String = "${date.isoLabel()}T${timeLabel()}:00"

private fun ShiftDay.shiftOccurrence(): ShiftOccurrence? {
    val shiftDate = date ?: return null
    val startMinute = type.startMinute ?: return null
    val endMinute = type.endMinute ?: return null
    val start = LabDateTime(shiftDate, startMinute)
    val endDate = if (type.crossesMidnight) shiftDate.plusDays(1) else shiftDate
    val end = LabDateTime(endDate, endMinute)
    return ShiftOccurrence(this, start, end, TemporalState.UPCOMING)
}

private fun pauseStartFor(occurrence: ShiftOccurrence, customTime: String?): LabDateTime? {
    val window = pauseWindowFor(occurrence) ?: return null
    if (customTime != null) {
        val minute = parseNotificationMinute(customTime) ?: return null
        val custom = LabDateTime(window.start.date, minute)
        return custom.takeIf { it >= window.start && it <= window.end }
    }
    return pauseSuggestionTimes(occurrence).firstOrNull()?.let { first ->
        LabDateTime(window.start.date, parseNotificationMinute(first) ?: return null)
    }
}

private fun scheduled(
    date: LabDate,
    type: NotificationType,
    triggerAt: LabDateTime,
    title: String,
    body: String
) = ScheduledNotification(
    id = "${date.isoLabel()}:${type.name}",
    type = type,
    triggerAt = triggerAt,
    title = title,
    body = body
)
