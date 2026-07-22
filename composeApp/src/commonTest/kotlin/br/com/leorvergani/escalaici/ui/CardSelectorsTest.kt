package br.com.leorvergani.escalaici.ui

import br.com.leorvergani.escalaici.model.LabDate
import br.com.leorvergani.escalaici.model.LabDateTime
import br.com.leorvergani.escalaici.model.NotificationSettings
import br.com.leorvergani.escalaici.model.ShiftDay
import br.com.leorvergani.escalaici.model.ShiftOccurrence
import br.com.leorvergani.escalaici.model.ShiftType
import br.com.leorvergani.escalaici.model.TemporalState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CardSelectorsTest {
    @Test
    fun effectivePauseUsesCustomTimeInsidePauseWindow() {
        val pause = effectivePause(
            shift = morningShift(),
            settings = NotificationSettings(notifyPause = true, pauseCustomTime = "10:30")
        )

        assertEquals("10:30–10:45", pause?.scheduledLabel)
        assertEquals("09:00", pause?.windowStart)
        assertEquals("11:45", pause?.windowEnd)
    }

    @Test
    fun effectivePauseFallsBackWhenCustomTimeIsNull() {
        val pause = effectivePause(
            shift = morningShift(),
            settings = NotificationSettings(notifyPause = true, pauseCustomTime = null)
        )

        assertNull(pause?.scheduledLabel)
        assertEquals("09:00", pause?.windowStart)
        assertEquals("11:45", pause?.windowEnd)
    }

    @Test
    fun effectivePauseFallsBackWhenPauseNotificationIsDisabled() {
        val pause = effectivePause(
            shift = morningShift(),
            settings = NotificationSettings(notifyPause = false, pauseCustomTime = "10:30")
        )

        assertNull(pause?.scheduledLabel)
        assertEquals("09:00", pause?.windowStart)
        assertEquals("11:45", pause?.windowEnd)
    }

    @Test
    fun effectivePauseFallsBackWhenCustomTimeIsOutsideWindow() {
        val pause = effectivePause(
            shift = morningShift(),
            settings = NotificationSettings(notifyPause = true, pauseCustomTime = "08:00")
        )

        assertNull(pause?.scheduledLabel)
        assertEquals("09:00", pause?.windowStart)
        assertEquals("11:45", pause?.windowEnd)
    }

    @Test
    fun effectivePauseFallsBackWhenCustomTimeIsInvalid() {
        val pause = effectivePause(
            shift = morningShift(),
            settings = NotificationSettings(notifyPause = true, pauseCustomTime = "abc")
        )

        assertNull(pause?.scheduledLabel)
        assertEquals("09:00", pause?.windowStart)
        assertEquals("11:45", pause?.windowEnd)
    }

    @Test
    fun effectivePauseReturnsNullWhenShiftIsNull() {
        assertNull(effectivePause(shift = null, settings = NotificationSettings()))
    }

    @Test
    fun colleaguesForShiftUsesOnlyMembersFromOwnShift() {
        val day = day(
            teamMembers = listOf("md1", "manha1", "manha2", "tarde1"),
            membersByShift = mapOf(
                ShiftType.MADRUGADA to listOf("md1"),
                ShiftType.MANHA to listOf("manha1", "manha2"),
                ShiftType.TARDE to listOf("tarde1")
            )
        )

        assertEquals(listOf("manha1", "manha2"), colleaguesForShift(day))
    }

    @Test
    fun colleaguesForShiftFallsBackToFlatTeamMembersWhenMembersByShiftIsEmpty() {
        val day = day(teamMembers = listOf("colega1", "colega2"), membersByShift = emptyMap())

        assertEquals(listOf("colega1", "colega2"), colleaguesForShift(day))
    }

    private fun morningShift(): ShiftOccurrence {
        val date = LabDate(2026, 7, 23)
        return ShiftOccurrence(
            day = day(date = date),
            start = LabDateTime(date, 7 * 60),
            end = LabDateTime(date, 13 * 60),
            state = TemporalState.CURRENT
        )
    }

    private fun day(
        date: LabDate = LabDate(2026, 7, 23),
        teamMembers: List<String> = emptyList(),
        membersByShift: Map<ShiftType, List<String>> = emptyMap()
    ) = ShiftDay(
        dayLabel = date.dayOfWeekShort(),
        dateLabel = date.dateLabel(),
        fullDateLabel = date.fullDateLabel(),
        type = ShiftType.MANHA,
        date = date,
        teamMembers = teamMembers,
        membersByShift = membersByShift
    )
}
