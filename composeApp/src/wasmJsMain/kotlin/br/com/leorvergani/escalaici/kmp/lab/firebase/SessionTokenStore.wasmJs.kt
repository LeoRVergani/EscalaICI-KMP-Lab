package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val StorageKey = "escalaici.firebase.session.v2"

/**
 * Persistencia via `localStorage` - mesmo nivel de exposicao que o proprio
 * SDK JS do Firebase ja aceita no navegador (nao ha Keystore/TEE
 * equivalente disponivel em Kotlin/Wasm hoje; ajuste aceito na aprovacao
 * do plano da FASE 15). Sempre limpo por completo no logout.
 */
private class WasmSessionTokenStore(private val json: Json = Json { ignoreUnknownKeys = true }) : SessionTokenStore {
    override fun read(): StoredAuthSession? {
        val raw = try { window.localStorage.getItem(StorageKey) } catch (_: Throwable) { null } ?: return null
        return runCatching { json.decodeFromString<StoredAuthSession>(raw) }.getOrNull()
    }

    override fun write(session: StoredAuthSession) {
        try {
            window.localStorage.setItem(StorageKey, json.encodeToString(session))
        } catch (_: Throwable) {
            // Sem storage disponivel (ex.: modo privado) - sessao so dura a aba atual.
        }
    }

    override fun clear() {
        try { window.localStorage.removeItem(StorageKey) } catch (_: Throwable) { }
    }
}

actual fun createSessionTokenStore(): SessionTokenStore = WasmSessionTokenStore()
