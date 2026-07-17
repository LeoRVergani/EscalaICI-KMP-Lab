package br.com.leorvergani.escalaici.platform

import androidx.compose.runtime.Composable
import br.com.leorvergani.escalaici.model.AppUpdateResult

/**
 * Verifica/baixa/instala atualização real do app — só faz sentido no
 * Android (a Web não instala APK; PWA se atualiza sozinho pelo navegador).
 * Espelha exatamente o fluxo do app oficial (`AppUpdateManager.kt`,
 * `EscalaSOC`, só leitura): manifesto JSON → compara versionCode → baixa
 * o APK → abre o instalador do sistema.
 */
interface AppUpdateChecker {
    suspend fun checkAndInstall(): AppUpdateResult
}

@Composable
expect fun rememberAppUpdateChecker(): AppUpdateChecker
