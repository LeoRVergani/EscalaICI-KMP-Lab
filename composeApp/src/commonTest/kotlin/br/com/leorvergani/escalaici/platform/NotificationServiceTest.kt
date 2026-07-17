package br.com.leorvergani.escalaici.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationServiceTest {
    @Test fun unsupportedServiceNeverPromptsOrShows() {
        assertEquals(NotificationPermissionState.UNSUPPORTED, UnsupportedWebNotificationService.capability().permissionState)
        var permission: NotificationPermissionState? = null
        var shown = true
        UnsupportedWebNotificationService.requestPermission { permission = it }
        UnsupportedWebNotificationService.showNotification("t", "b", "tag") { shown = it }
        assertEquals(NotificationPermissionState.UNSUPPORTED, permission)
        assertFalse(shown)
    }

    @Test fun fakeRequiresExplicitClickBeforeTestNotification() {
        val fake = FakeNotifications(NotificationPermissionState.DEFAULT)
        assertEquals(0, fake.permissionRequests)
        fake.requestPermission { assertEquals(NotificationPermissionState.GRANTED, it) }
        assertEquals(1, fake.permissionRequests)
        fake.showNotification("Pausa do turno", "Teste", "stable-tag") { assertTrue(it) }
        assertEquals(listOf("stable-tag"), fake.tags)
    }

    @Test fun webCapabilitiesDoNotPromisePushOrClosedBrowserScheduling() {
        val capabilities = PlatformCapabilities(
            supportsAppUpdate = false,
            supportsWebNotifications = true,
            supportsPushNotifications = false,
            supportsBackgroundScheduledNotifications = false
        )
        assertTrue(capabilities.supportsWebNotifications)
        assertFalse(capabilities.supportsAppUpdate)
        assertFalse(capabilities.supportsPushNotifications)
        assertFalse(capabilities.supportsBackgroundScheduledNotifications)
    }

    private class FakeNotifications(private var state: NotificationPermissionState) : WebNotificationService {
        var permissionRequests = 0
        val tags = mutableListOf<String>()
        override fun capability() = NotificationCapability(state, true)
        override fun requestPermission(onResult: (NotificationPermissionState) -> Unit) {
            permissionRequests += 1
            state = NotificationPermissionState.GRANTED
            onResult(state)
        }
        override fun showNotification(title: String, body: String, tag: String, onResult: (Boolean) -> Unit) {
            if (state == NotificationPermissionState.GRANTED) tags += tag
            onResult(state == NotificationPermissionState.GRANTED)
        }
        override fun cancelNotification(tag: String) { tags.remove(tag) }
    }
}
