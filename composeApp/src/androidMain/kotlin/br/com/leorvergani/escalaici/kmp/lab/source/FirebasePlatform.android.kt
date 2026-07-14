package br.com.leorvergani.escalaici.kmp.lab.source

import android.content.Context

private lateinit var applicationContext: Context

fun initializeFirebasePlatform(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createFirebaseScheduleGateway(): FirebaseScheduleGateway = FirestoreRestGateway()

actual fun createFirebaseRawCacheStore(): FirebaseRawCacheStore {
    check(::applicationContext.isInitialized) { "Firebase platform não inicializada." }
    val preferences = applicationContext.getSharedPreferences("escalaici.firebase.cache", Context.MODE_PRIVATE)
    return object : FirebaseRawCacheStore {
        override fun read(key: String): String? = preferences.getString(key, null)
        override fun write(key: String, value: String): Boolean = preferences.edit().putString(key, value).commit()
        override fun remove(key: String) { preferences.edit().remove(key).apply() }
    }
}
