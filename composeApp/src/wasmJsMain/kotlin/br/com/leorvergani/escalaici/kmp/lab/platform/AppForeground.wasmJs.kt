package br.com.leorvergani.escalaici.kmp.lab.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * `document.visibilitychange` e o equivalente Web de "app voltou ao
 * foreground" - só dispara `onForeground` quando `visibilityState` vira
 * `"visible"` (não em toda troca de aba/minimização). O handler retornado
 * por [registerVisibilityListener] é guardado para ser removido em
 * [unregisterVisibilityListener] no `onDispose` - nunca deixa um listener
 * duplicado entre recomposições.
 */
@Composable
actual fun ObserveAppForeground(onForeground: () -> Unit) {
    val currentOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(Unit) {
        val handler = registerVisibilityListener { currentOnForeground() }
        onDispose { unregisterVisibilityListener(handler) }
    }
}

private fun registerVisibilityListener(callback: () -> Unit): JsAny = js(
    "(function() { var handler = function() { if (document.visibilityState === 'visible') { callback(); } }; document.addEventListener('visibilitychange', handler); return handler; })()",
)

private fun unregisterVisibilityListener(handler: JsAny) {
    js("document.removeEventListener('visibilitychange', handler)")
}
