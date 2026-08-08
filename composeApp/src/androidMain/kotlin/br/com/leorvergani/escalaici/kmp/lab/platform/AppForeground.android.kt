package br.com.leorvergani.escalaici.kmp.lab.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * `ProcessLifecycleOwner` reflete o app inteiro (nao uma Activity so) -
 * `ON_START` dispara quando o processo volta ao foreground, inclusive apos
 * troca de Activity/rotacao (que nao deveria contar como "voltou ao
 * foreground"). Registrado/removido via `DisposableEffect`, nunca duplicado
 * entre recomposicoes.
 */
@Composable
actual fun ObserveAppForeground(onForeground: () -> Unit) {
    val currentOnForeground by rememberUpdatedState(onForeground)
    DisposableEffect(Unit) {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) currentOnForeground()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
}
