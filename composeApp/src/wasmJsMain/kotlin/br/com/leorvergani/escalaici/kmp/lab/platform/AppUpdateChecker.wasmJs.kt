package br.com.leorvergani.escalaici.kmp.lab.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import br.com.leorvergani.escalaici.kmp.lab.model.AppUpdateResult

/** Instalação de APK não existe na Web — o app oficial também não tem essa funcionalidade fora do Android. */
@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker {
    return remember {
        object : AppUpdateChecker {
            override suspend fun checkAndInstall(): AppUpdateResult = AppUpdateResult.NotSupported
        }
    }
}
