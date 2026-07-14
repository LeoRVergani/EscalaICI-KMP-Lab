package br.com.leorvergani.escalaici.kmp.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.leorvergani.escalaici.kmp.lab.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.kmp.lab.source.FirebaseSourceCache
import br.com.leorvergani.escalaici.kmp.lab.source.createFirebaseRawCacheStore
import br.com.leorvergani.escalaici.kmp.lab.source.createFirebaseScheduleGateway
import br.com.leorvergani.escalaici.kmp.lab.source.initializeFirebasePlatform

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebasePlatform(this)
        setContent {
            EscalaIciLabApp(
                firebaseGateway = createFirebaseScheduleGateway(),
                firebaseCache = FirebaseSourceCache(createFirebaseRawCacheStore()),
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true
                )
            )
        }
    }
}
