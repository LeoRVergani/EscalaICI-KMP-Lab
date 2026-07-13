package br.com.leorvergani.escalaici.kmp.lab

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.kmp.lab.repository.WebLocalDataCache
import br.com.leorvergani.escalaici.kmp.lab.platform.WebCurrentTimeProvider
import br.com.leorvergani.escalaici.kmp.lab.platform.PlatformCapabilities

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        EscalaIciLabApp(localDataCache = WebLocalDataCache(), currentTimeProvider = WebCurrentTimeProvider, platformCapabilities = PlatformCapabilities(false))
    }
}
