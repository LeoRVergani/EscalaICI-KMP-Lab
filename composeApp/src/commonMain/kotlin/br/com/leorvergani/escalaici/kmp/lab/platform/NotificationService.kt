package br.com.leorvergani.escalaici.kmp.lab.platform

enum class NotificationPermissionState { DEFAULT, GRANTED, DENIED, UNSUPPORTED }

data class NotificationCapability(
    val permissionState: NotificationPermissionState,
    val supportsSystemNotifications: Boolean,
    val supportsReliableBackgroundScheduling: Boolean = false
)

interface WebNotificationService {
    fun capability(): NotificationCapability
    fun requestPermission(onResult: (NotificationPermissionState) -> Unit)
    fun showNotification(title: String, body: String, tag: String, onResult: (Boolean) -> Unit)
    fun cancelNotification(tag: String)
}

object UnsupportedWebNotificationService : WebNotificationService {
    override fun capability() = NotificationCapability(NotificationPermissionState.UNSUPPORTED, false)
    override fun requestPermission(onResult: (NotificationPermissionState) -> Unit) = onResult(NotificationPermissionState.UNSUPPORTED)
    override fun showNotification(title: String, body: String, tag: String, onResult: (Boolean) -> Unit) = onResult(false)
    override fun cancelNotification(tag: String) = Unit
}
