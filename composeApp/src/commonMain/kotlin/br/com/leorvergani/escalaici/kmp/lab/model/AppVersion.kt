package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Mantido manualmente em sincronia com `versionCode`/`versionName` em
 * `composeApp/build.gradle.kts` — Kotlin Multiplatform nao gera um
 * BuildConfig acessivel de `commonMain` sem plugin adicional, entao estas
 * constantes sao a fonte exibida na aba Perfil e usada na comparacao de
 * atualizacao (`AppUpdateChecker`, igual ao `BuildConfig.VERSION_CODE` do
 * app oficial).
 */
object AppVersion {
    const val CODE: Int = 12
    const val LABEL: String = "0.6.2"
}
