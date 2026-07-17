package br.com.leorvergani.escalaici.platform

class BrowserNotificationService : WebNotificationService {
    override fun capability(): NotificationCapability {
        val supported = notificationsSupported() && serviceWorkerSupported() && secureContext()
        return NotificationCapability(if (supported) permissionState() else NotificationPermissionState.UNSUPPORTED, supported)
    }

    override fun requestPermission(onResult: (NotificationPermissionState) -> Unit) {
        if (!capability().supportsSystemNotifications) return onResult(NotificationPermissionState.UNSUPPORTED)
        requestBrowserPermission { onResult(it.toPermissionState()) }
    }

    override fun showNotification(title: String, body: String, tag: String, onResult: (Boolean) -> Unit) {
        if (capability().permissionState != NotificationPermissionState.GRANTED) return onResult(false)
        showViaServiceWorker(title, body, tag, onResult)
    }

    override fun cancelNotification(tag: String) { closeByTag(tag) }
}

private fun String.toPermissionState() = when (this) {
    "granted" -> NotificationPermissionState.GRANTED
    "denied" -> NotificationPermissionState.DENIED
    else -> NotificationPermissionState.DEFAULT
}
private fun permissionState(): NotificationPermissionState = browserPermission().toPermissionState()
private fun notificationsSupported(): Boolean = js("typeof Notification !== 'undefined'")
private fun serviceWorkerSupported(): Boolean = js("'serviceWorker' in navigator")
private fun secureContext(): Boolean = js("window.isSecureContext === true")
private fun browserPermission(): String = js("Notification.permission")
private fun requestBrowserPermission(callback: (String) -> Unit) { js("Notification.requestPermission().then(callback).catch(function(){ callback('denied'); })") }
private fun showViaServiceWorker(title: String, body: String, tag: String, callback: (Boolean) -> Unit) {
    js("navigator.serviceWorker.ready.then(function(reg){ return reg.showNotification(title, { body: body, tag: tag, icon: './icons/icon-192.png', badge: './icons/icon-192.png', data: { route: 'hoje' } }); }).then(function(){ callback(true); }).catch(function(){ callback(false); })")
}
private fun closeByTag(tag: String) {
    js("navigator.serviceWorker.ready.then(function(reg){ return reg.getNotifications({tag: tag}); }).then(function(items){ items.forEach(function(item){ item.close(); }); })")
}
