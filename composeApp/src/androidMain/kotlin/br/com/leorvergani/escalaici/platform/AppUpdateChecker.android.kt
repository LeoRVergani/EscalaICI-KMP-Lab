package br.com.leorvergani.escalaici.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import br.com.leorvergani.escalaici.model.AppUpdateConfig
import br.com.leorvergani.escalaici.model.AppUpdateResult
import br.com.leorvergani.escalaici.model.AppVersion
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File

@Composable
actual fun rememberAppUpdateChecker(): AppUpdateChecker {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidAppUpdateChecker(context) }
}

/**
 * Porte fiel do fluxo real (`AppUpdateManager.kt`, `EscalaSOC`, só
 * leitura): mesmo `org.json.JSONObject` manual (não kotlinx.serialization),
 * mesma comparação simples de `versionCode`, mesmo destino de download
 * (`cacheDir/updates/`), mesmo `FileProvider` + `ACTION_VIEW` para abrir o
 * instalador, mesma checagem de permissão "instalar apps desconhecidos"
 * antes de baixar.
 */
private class AndroidAppUpdateChecker(private val context: Context) : AppUpdateChecker {
    override suspend fun checkAndInstall(): AppUpdateResult {
        return try {
            val manifestJson = JSONObject(downloadBytes(AppUpdateConfig.MANIFEST_URL).decodeToString())
            val versionCode = manifestJson.optInt("kmpVersionCode", 0)
            val versionName = manifestJson.optString("kmpVersionName")
            val apkUrl = manifestJson.optString("kmpApkUrl")
            val changelog = manifestJson.optString("kmpChangelog").takeIf { it.isNotBlank() }

            if (versionCode <= AppVersion.CODE || apkUrl.isBlank()) {
                return AppUpdateResult.UpToDate
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                openUnknownSourcesSettings()
                return AppUpdateResult.PermissionRequired
            }

            val apkFile = downloadApk(apkUrl)
            openInstaller(apkFile)
            AppUpdateResult.InstallStarted(versionName, changelog)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            // Throwable (não só Exception): um APK de atualização pode ter
            // 60-100MB — se o download ainda assim estourar memória em um
            // aparelho mais fraco, um OutOfMemoryError não seria capturado
            // por `catch (Exception)`, derrubando o app inteiro sem
            // nenhuma mensagem (o que se via como "o app fecha sozinho").
            // Mesmo padrão do app oficial (`AppUpdateManager`, também
            // `catch (Throwable)`).
            AppUpdateResult.Failure("Não foi possível verificar ou baixar a atualização.")
        }
    }

    private suspend fun downloadApk(apkUrl: String): File {
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val apkFile = File(updatesDir, "EscalaICI-latest.apk")
        if (apkFile.exists()) apkFile.delete()
        downloadToFile(apkUrl, apkFile)
        if (!apkFile.exists() || apkFile.length() <= 0L) {
            apkFile.delete()
            error("APK inválido.")
        }
        return apkFile
    }

    private fun openUnknownSourcesSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun openInstaller(apk: File) {
        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
