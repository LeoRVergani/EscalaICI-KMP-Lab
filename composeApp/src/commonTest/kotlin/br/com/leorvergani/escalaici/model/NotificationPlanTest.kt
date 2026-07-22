package br.com.leorvergani.escalaici.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPlanTest {
    @Test
    fun workDayGeneratesDayBeforeShiftStartShiftEndAndPauseEvents() {
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.MANHA)),
            settings = NotificationSettings(),
            now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)
        )

        assertEquals(
            listOf(
                NotificationType.DAY_BEFORE_WORK,
                NotificationType.SHIFT_START,
                NotificationType.PAUSE_START,
                NotificationType.PAUSE_END,
                NotificationType.SHIFT_END
            ),
            plan.map { it.type }
        )
        assertEquals(LabDateTime(LabDate(2026, 7, 22), 18 * 60), plan.first { it.type == NotificationType.DAY_BEFORE_WORK }.triggerAt)
        assertEquals(LabDateTime(LabDate(2026, 7, 23), 6 * 60 + 30), plan.first { it.type == NotificationType.SHIFT_START }.triggerAt)
        assertEquals(LabDateTime(LabDate(2026, 7, 23), 13 * 60), plan.first { it.type == NotificationType.SHIFT_END }.triggerAt)
        assertEquals(LabDateTime(LabDate(2026, 7, 23), 9 * 60), plan.first { it.type == NotificationType.PAUSE_START }.triggerAt)
        assertEquals(LabDateTime(LabDate(2026, 7, 23), 9 * 60 + PauseDurationMinutes), plan.first { it.type == NotificationType.PAUSE_END }.triggerAt)
    }

    @Test
    fun restDayGeneratesOnlyDayBeforeRest() {
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.FOLGA)),
            settings = NotificationSettings(),
            now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)
        )

        assertEquals(listOf(NotificationType.DAY_BEFORE_REST), plan.map { it.type })
        assertEquals(LabDateTime(LabDate(2026, 7, 22), 18 * 60), plan.single().triggerAt)
    }

    @Test
    fun pastEventsAreNeverReturned() {
        val now = LabDateTime(LabDate(2026, 7, 23), 10 * 60)
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.MANHA)),
            settings = NotificationSettings(),
            now = now
        )

        assertTrue(plan.all { it.triggerAt >= now })
        assertFalse(plan.any { it.type == NotificationType.DAY_BEFORE_WORK })
        assertFalse(plan.any { it.type == NotificationType.SHIFT_START })
        assertFalse(plan.any { it.type == NotificationType.PAUSE_START })
    }

    @Test
    fun nightShiftEndFallsOnNextDay() {
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.NOITE)),
            settings = NotificationSettings(),
            now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)
        )

        assertEquals(LabDateTime(LabDate(2026, 7, 24), 60), plan.first { it.type == NotificationType.SHIFT_END }.triggerAt)
    }

    @Test
    fun invalidPauseCustomTimeIsIgnored() {
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.MANHA)),
            settings = NotificationSettings(pauseCustomTime = "07:00"),
            now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)
        )

        assertFalse(plan.any { it.type == NotificationType.PAUSE_START || it.type == NotificationType.PAUSE_END })
    }

    @Test
    fun deterministicIdsAreStableForSameInput() {
        val days = listOf(day(LabDate(2026, 7, 23), ShiftType.MANHA), day(LabDate(2026, 7, 24), ShiftType.FOLGA))
        val settings = NotificationSettings(dayBeforeTime = "17:30", shiftStartOffsetMinutes = 15, pauseCustomTime = "09:30")
        val now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)

        val first = buildNotificationPlan(days, settings, now).map { it.id }
        val second = buildNotificationPlan(days, settings, now).map { it.id }

        assertEquals(first, second)
        assertEquals(first.toSet().size, first.size)
    }

    @Test
    fun pauseEventsAreNotScheduledWhenSinglePauseFlagIsFalse() {
        val plan = buildNotificationPlan(
            days = listOf(day(LabDate(2026, 7, 23), ShiftType.MANHA)),
            settings = NotificationSettings(notifyPause = false),
            now = LabDateTime(LabDate(2026, 7, 22), 8 * 60)
        )

        assertFalse(plan.any { it.type == NotificationType.PAUSE_START || it.type == NotificationType.PAUSE_END })
    }

    private fun day(date: LabDate, type: ShiftType) = ShiftDay(
        dayLabel = date.dayOfWeekShort(),
        dateLabel = date.dateLabel(),
        fullDateLabel = date.fullDateLabel(),
        type = type,
        date = date
    )
}
