package br.com.leorvergani.escalaici.kmp.lab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import br.com.leorvergani.escalaici.kmp.lab.platform.PlatformCapabilities
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscalaIciLabApp(
                platformCapabilities = PlatformCapabilities(
                    supportsAppUpdate = true
                )
            )
        }
    }
}
