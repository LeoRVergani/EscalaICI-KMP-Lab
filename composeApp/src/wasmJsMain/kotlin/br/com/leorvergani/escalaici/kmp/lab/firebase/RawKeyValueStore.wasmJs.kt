package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.browser.window

actual fun createRawKeyValueStore(): RawKeyValueStore = object : RawKeyValueStore {
    override fun read(key: String): String? = try { window.localStorage.getItem(key) } catch (_: Throwable) { null }
    override fun write(key: String, value: String): Boolean = try { window.localStorage.setItem(key, value); true } catch (_: Throwable) { false }
    override fun remove(key: String) { try { window.localStorage.removeItem(key) } catch (_: Throwable) { } }
}
