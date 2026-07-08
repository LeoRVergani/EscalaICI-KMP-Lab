package br.com.leorvergani.escalaici.kmp.lab

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import br.com.leorvergani.escalaici.kmp.lab.ui.EscalaIciLabApp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "webApp") {
        EscalaIciLabApp()
    }
}
