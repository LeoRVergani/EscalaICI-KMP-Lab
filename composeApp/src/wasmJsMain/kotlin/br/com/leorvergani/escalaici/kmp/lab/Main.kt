package br.com.leorvergani.escalaici.kmp.lab

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp
import br.com.leorvergani.escalaici.kmp.lab.repository.WebLocalDataCache

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        EscalaIciLabApp(localDataCache = WebLocalDataCache())
    }
}
