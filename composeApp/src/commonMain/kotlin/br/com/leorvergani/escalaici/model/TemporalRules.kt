package br.com.leorvergani.escalaici.model

data class LabDateTime(val date: LabDate, val minuteOfDay: Int) : Comparable<LabDateTime> {
    init { require(minuteOfDay in 0 until 24 * 60) }
    override fun compareTo(other: LabDateTime): Int =
        compareValuesBy(this, other, { it.date }, { it.minuteOfDay })
}

enum class TemporalState { CURRENT, UPCOMING, FINISHED, NONE }

data class ShiftOccurrence(val day: ShiftDay, val start: LabDateTime, val end: LabDateTime, val state: TemporalState)

fun ScheduleSummary.relevantShift(now: LabDateTime): ShiftOccurrence? = days
    .mapNotNull { day ->
        val date = day.date ?: return@mapNotNull null
        val startMinute = day.type.startMinute ?: return@mapNotNull null
        val endMinute = day.type.endMinute ?: return@mapNotNull null
        val start = LabDateTime(date, startMinute)
        val endDate = if (day.type.crossesMidnight) date.plusDays(1) else date
        val end = LabDateTime(endDate, endMinute)
        val state = when {
            now >= start && now < end -> TemporalState.CURRENT
            now < start -> TemporalState.UPCOMING
            else -> TemporalState.FINISHED
        }
        ShiftOccurrence(day, start, end, state)
    }
    .sortedBy { it.start }
    .let { occurrences -> occurrences.firstOrNull { it.state == TemporalState.CURRENT } ?: occurrences.firstOrNull { it.state == TemporalState.UPCOMING } }

data class PausePresentation(val scheduledLabel: String?, val windowStart: String, val windowEnd: String) {
    val displayTitle: String get() = if (scheduledLabel != null) "Pausa programada" else "Janela de pausa"
    val displayValue: String get() = scheduledLabel ?: "$windowStart–$windowEnd"
}

fun pauseFor(shift: ShiftOccurrence?): PausePresentation? {
    val start = shift?.day?.type?.startMinute ?: return null
    val end = shift.end
    val windowStart = shift.start.plusMinutes(120)
    val windowEnd = shift.start.plusMinutes(285)
    if (windowStart > end) return null
    return PausePresentation(
        scheduledLabel = null,
        windowStart = windowStart.timeLabel(),
        windowEnd = minOf(windowEnd, end).timeLabel()
    )
}

data class OnCallOccurrence(val assignment: OnCallAssignment, val start: LabDateTime, val end: LabDateTime, val state: TemporalState)

fun relevantOnCall(assignments: List<OnCallAssignment>, now: LabDateTime): OnCallOccurrence? = assignments
    .mapNotNull { it.occurrence(now) }
    .sortedBy { it.start }
    .let { values -> values.firstOrNull { it.state == TemporalState.CURRENT } ?: values.firstOrNull { it.state == TemporalState.UPCOMING } }

fun OnCallAssignment.occurrence(now: LabDateTime): OnCallOccurrence? {
    val start = LabDate.parseIso(startDate)?.let { LabDateTime(it, parseMinute(startTime) ?: return null) } ?: return null
    val end = LabDate.parseIso(endDate)?.let { LabDateTime(it, parseMinute(endTime) ?: return null) } ?: return null
    val state = when {
        now >= start && now < end -> TemporalState.CURRENT
        now < start -> TemporalState.UPCOMING
        else -> TemporalState.FINISHED
    }
    return OnCallOccurrence(this, start, end, state)
}

fun onCallDates(assignment: OnCallAssignment): Set<LabDate> {
    val start = LabDate.parseIso(assignment.startDate) ?: return emptySet()
    val end = LabDate.parseIso(assignment.endDate) ?: return emptySet()
    if (end < start) return emptySet()
    val result = mutableSetOf<LabDate>()
    var date = start
    while (date <= end) { result += date; date = date.plusDays(1) }
    return result
}

fun LabDate.plusDays(days: Int): LabDate {
    var result = this
    repeat(days) {
        result = if (result.day < LabDate.monthLength(result.year, result.month)) result.copy(day = result.day + 1)
        else if (result.month < 12) LabDate(result.year, result.month + 1, 1)
        else LabDate(result.year + 1, 1, 1)
    }
    return result
}

private fun LabDateTime.plusMinutes(minutes: Int): LabDateTime {
    val total = minuteOfDay + minutes
    return LabDateTime(date.plusDays(total / (24 * 60)), total % (24 * 60))
}

fun LabDateTime.timeLabel(): String = "${(minuteOfDay / 60).toString().padStart(2, '0')}:${(minuteOfDay % 60).toString().padStart(2, '0')}"
private fun parseMinute(value: String): Int? = value.split(":").takeIf { it.size == 2 }?.let { p ->
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    (h * 60 + m).takeIf { h in 0..23 && m in 0..59 }
}
