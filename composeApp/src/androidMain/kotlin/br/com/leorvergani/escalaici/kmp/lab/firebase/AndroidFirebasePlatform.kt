package br.com.leorvergani.escalaici.kmp.lab.firebase

import android.content.Context

/**
 * Contexto de aplicacao Android necessario para `SharedPreferences`/Android
 * Keystore usados por [SessionTokenStore]. Inicializado uma vez em
 * `MainActivity.onCreate` - mesmo padrao do antigo
 * `initializeFirebasePlatform` (removido junto com o schema antigo nesta
 * fase).
 */
internal object AndroidFirebasePlatform {
    lateinit var applicationContext: Context
        private set

    fun isInitialized(): Boolean = ::applicationContext.isInitialized

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }
}

fun initializeAndroidFirebasePlatform(context: Context) {
    AndroidFirebasePlatform.initialize(context)
}
