package br.com.leorvergani.escalaici

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.leorvergani.escalaici.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.source.createFirebaseScheduleGateway
import br.com.leorvergani.escalaici.source.initializeFirebasePlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebasePlatform(this)
        setContent {
            EscalaIciLabApp(
                firebaseGateway = createFirebaseScheduleGateway(),
                firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true,
                    supportsCorporateAuth = true
                )
            )
        }
    }
}
