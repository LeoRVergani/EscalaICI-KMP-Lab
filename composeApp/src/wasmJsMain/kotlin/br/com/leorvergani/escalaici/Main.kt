package br.com.leorvergani.escalaici

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.repository.WebLocalDataCache
import br.com.leorvergani.escalaici.platform.WebCurrentTimeProvider
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.platform.BrowserNotificationService
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        val notifications = BrowserNotificationService()
        EscalaIciLabApp(
            firebaseGateway = createFirebaseScheduleGateway(),
            firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
            localDataCache = WebLocalDataCache(),
            currentTimeProvider = WebCurrentTimeProvider,
            platformCapabilities = PlatformCapabilities(
                supportsAppUpdate = false,
                supportsWebNotifications = notifications.capability().supportsSystemNotifications
            ),
            notificationService = notifications
        )
    }
}
