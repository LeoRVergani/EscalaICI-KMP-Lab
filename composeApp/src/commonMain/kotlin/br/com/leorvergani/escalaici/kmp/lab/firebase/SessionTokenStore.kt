package br.com.leorvergani.escalaici.kmp.lab.firebase

import kotlinx.serialization.Serializable

/**
 * Sessao Firebase Auth persistida localmente. So um slot por dispositivo -
 * o app so suporta um usuario logado por vez, e o logout limpa este slot
 * por completo (nunca deixa sessao de um usuario "vazar" para o proximo
 * login, ver prompt FASE 15 secao 19/43).
 */
@Serializable
data class StoredAuthSession(
    val login: String,
    val email: String,
    val idToken: String,
    val refreshToken: String,
)

/**
 * Armazenamento do token de sessao. Android: criptografado via Android
 * Keystore (AES-GCM) antes de gravar em SharedPreferences - nunca texto
 * simples (ajuste obrigatorio definido na aprovacao do plano da FASE 15).
 * Web/Wasm: `localStorage` (mesmo nivel de exposicao que o proprio SDK JS
 * do Firebase aceita no navegador), limpo por completo no logout.
 */
interface SessionTokenStore {
    fun read(): StoredAuthSession?
    fun write(session: StoredAuthSession)
    fun clear()
}

expect fun createSessionTokenStore(): SessionTokenStore
