package br.com.leorvergani.escalaici.kmp.lab.firebase

import android.content.Context

private const val PreferencesName = "escalaici.firebase.schedule.cache"

actual fun createRawKeyValueStore(): RawKeyValueStore {
    check(AndroidFirebasePlatform.isInitialized()) {
        "AndroidFirebasePlatform nao inicializada - chame initializeAndroidFirebasePlatform(context) em MainActivity.onCreate."
    }
    val preferences = AndroidFirebasePlatform.applicationContext.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
    return object : RawKeyValueStore {
        override fun read(key: String): String? = preferences.getString(key, null)
        override fun write(key: String, value: String): Boolean = preferences.edit().putString(key, value).commit()
        override fun remove(key: String) { preferences.edit().remove(key).apply() }
    }
}
