package br.com.leorvergani.escalaici.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalNotificationRuntimeTest {
    @Test
    fun requestCodeIsDeterministicForNotificationId() {
        val id = "2026-07-23:SHIFT_START"

        assertEquals(notificationRequestCodeFor(id), notificationRequestCodeFor(id))
        assertEquals(id.hashCode(), notificationRequestCodeFor(id))
    }

    @Test
    fun reconciliationCancelsRemovedIdsAndSchedulesCurrentPlan() {
        val previous = setOf("2026-07-23:SHIFT_START", "2026-07-23:PAUSE_START", "2026-07-24:SHIFT_END")
        val next = setOf("2026-07-23:SHIFT_START", "2026-07-25:SHIFT_START")

        val result = reconcileNotificationIds(previous, next)

        assertEquals(setOf("2026-07-23:PAUSE_START", "2026-07-24:SHIFT_END"), result.idsToCancel)
        assertEquals(next, result.idsToSchedule)
        assertEquals(setOf("2026-07-23:SHIFT_START"), result.idsKept)
    }

    @Test
    fun grantedPostNotificationPermissionAvoidsRuntimeRequest() {
        assertFalse(
            shouldRequestPostNotifications(
                sdkInt = 33,
                permissionGranted = true,
                alreadyRequested = false
            )
        )
    }

    @Test
    fun postNotificationRequestIsOnlyNeededOnAndroid13PlusBeforeFirstRequest() {
        assertFalse(shouldRequestPostNotifications(sdkInt = 32, permissionGranted = false, alreadyRequested = false))
        assertTrue(shouldRequestPostNotifications(sdkInt = 33, permissionGranted = false, alreadyRequested = false))
        assertFalse(shouldRequestPostNotifications(sdkInt = 33, permissionGranted = false, alreadyRequested = true))
    }
}
