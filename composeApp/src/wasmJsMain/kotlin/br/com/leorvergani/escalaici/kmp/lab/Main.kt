package br.com.leorvergani.escalaici.kmp.lab

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.kmp.lab.repository.WebLocalDataCache
import br.com.leorvergani.escalaici.kmp.lab.platform.WebCurrentTimeProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.kmp.lab.platform.BrowserNotificationService
import br.com.leorvergani.escalaici.kmp.lab.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.kmp.lab.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.kmp.lab.source.createFirebaseScheduleGateway

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
