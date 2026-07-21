package br.com.leorvergani.escalaici.source

import android.content.Context
import br.com.leorvergani.escalaici.BuildConfig

private lateinit var applicationContext: Context

fun initializeFirebasePlatform(context: Context) {
    applicationContext = context.applicationContext
}

actual fun createFirebaseScheduleGateway(): FirebaseScheduleGateway = FirestoreRestGateway()

actual fun createDemoPublicationGateway(): DemoPublicationGateway = createConfiguredDemoPublicationGateway()

actual fun platformDemoFirebaseConfig(): DemoFirebaseConfig =
    DemoFirebaseConfig(
        projectId = BuildConfig.DEMO_FIREBASE_PROJECT_ID
    )

actual fun createFirebaseRawCacheStore(): FirebaseRawCacheStore {
    check(::applicationContext.isInitialized) { "Firebase platform não inicializada." }
    val preferences = applicationContext.getSharedPreferences("escalaici.firebase.cache", Context.MODE_PRIVATE)
    return object : FirebaseRawCacheStore {
        override fun read(key: String): String? = preferences.getString(key, null)
        override fun write(key: String, value: String): Boolean = preferences.edit().putString(key, value).commit()
        override fun remove(key: String) { preferences.edit().remove(key).apply() }
    }
}
