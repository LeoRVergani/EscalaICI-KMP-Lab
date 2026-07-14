package br.com.leorvergani.escalaici.kmp.lab.source

import kotlinx.browser.window

actual fun createFirebaseScheduleGateway(): FirebaseScheduleGateway = FirestoreRestGateway()

actual fun createFirebaseRawCacheStore(): FirebaseRawCacheStore = object : FirebaseRawCacheStore {
    override fun read(key: String): String? = try { window.localStorage.getItem(key) } catch (_: Throwable) { null }
    override fun write(key: String, value: String): Boolean = try { window.localStorage.setItem(key, value); true } catch (_: Throwable) { false }
    override fun remove(key: String) { try { window.localStorage.removeItem(key) } catch (_: Throwable) { } }
}
