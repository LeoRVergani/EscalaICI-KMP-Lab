package br.com.leorvergani.escalaici.model

sealed interface AppUpdateResult {
    data object UpToDate : AppUpdateResult
    data class InstallStarted(val versionName: String, val changelog: String?) : AppUpdateResult
    data object PermissionRequired : AppUpdateResult
    data object NotSupported : AppUpdateResult
    data class Failure(val message: String) : AppUpdateResult
}

/**
 * Mesmo `version.json` já usado pelo app Android oficial (`EscalaSOC`,
 * `DropboxCloudConfig.APP_UPDATE_MANIFEST_URL`) — reaproveitado, não
 * duplicado. O parser oficial (`AppUpdateManager.fetchManifest()`, so
 * leitura) usa `org.json.JSONObject` com `optInt`/`optString`, que ignora
 * em silêncio qualquer chave que não conhece — confirmado lendo o código
 * real, então os campos `kmp*` abaixo podem conviver no mesmo arquivo sem
 * quebrar o app oficial (que já convive hoje com um campo extra
 * `releaseNotes` que ele também não lê).
 *
 * Campos lidos por este app (nunca reutilizar os nomes do app oficial:
 * `versionCode`/`versionName`/`apkUrl`/`changelog`):
 * `kmpVersionCode` (Int), `kmpVersionName` (String), `kmpApkUrl` (String),
 * `kmpChangelog` (String, opcional).
 */
object AppUpdateConfig {
    const val MANIFEST_URL: String =
        "https://www.dropbox.com/scl/fi/36m3c87z17vr7ookglthh/version.json?rlkey=2ouklj4ns7gu2mwy0e64jftfi&st=crwhdhpl&dl=1"
}
