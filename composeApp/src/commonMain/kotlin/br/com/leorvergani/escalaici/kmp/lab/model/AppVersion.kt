package br.com.leorvergani.escalaici.kmp.lab.model

/**
 * Mantido manualmente em sincronia com `versionName` em
 * `composeApp/build.gradle.kts` — Kotlin Multiplatform nao gera um
 * BuildConfig acessivel de `commonMain` sem plugin adicional, entao esta
 * constante e a fonte exibida na aba Perfil.
 */
object AppVersion {
    const val LABEL: String = "0.5.0"
}
