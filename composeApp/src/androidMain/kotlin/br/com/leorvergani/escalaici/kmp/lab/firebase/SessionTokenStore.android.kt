package br.com.leorvergani.escalaici.kmp.lab.firebase

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val KeyAlias = "escalaici_session_key"
private const val PreferencesName = "escalaici.firebase.session"
private const val PreferencesKey = "session_v2"
private const val GcmTagLengthBits = 128

/**
 * Sessao criptografada com uma chave do Android Keystore (AES-256/GCM,
 * `PURPOSE_ENCRYPT/DECRYPT`) - a chave privada nunca sai do hardware/TEE do
 * dispositivo, so o blob cifrado fica em `SharedPreferences`. Substitui o
 * uso de `SharedPreferences` em texto simples (ajuste obrigatorio definido
 * na aprovacao do plano da FASE 15).
 */
private class AndroidSessionTokenStore(private val context: Context) : SessionTokenStore {
    private val json = Json { ignoreUnknownKeys = true }
    private val preferences by lazy { context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE) }

    override fun read(): StoredAuthSession? {
        val encoded = preferences.getString(PreferencesKey, null) ?: return null
        return runCatching {
            val raw = decrypt(encoded)
            json.decodeFromString<StoredAuthSession>(raw)
        }.getOrNull()
    }

    override fun write(session: StoredAuthSession) {
        val raw = json.encodeToString(session)
        preferences.edit().putString(PreferencesKey, encrypt(raw)).apply()
    }

    override fun clear() {
        preferences.edit().remove(PreferencesKey).apply()
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherBytes = cipher.doFinal(plainText.encodeToByteArray())
        val iv = cipher.iv
        return "${Base64.encodeToString(iv, Base64.NO_WRAP)}:${Base64.encodeToString(cipherBytes, Base64.NO_WRAP)}"
    }

    private fun decrypt(encoded: String): String {
        val (ivPart, cipherPart) = encoded.split(":", limit = 2).let { it[0] to it[1] }
        val iv = Base64.decode(ivPart, Base64.NO_WRAP)
        val cipherBytes = Base64.decode(cipherPart, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GcmTagLengthBits, iv))
        return cipher.doFinal(cipherBytes).decodeToString()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KeyAlias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        keyGenerator.init(
            KeyGenParameterSpec.Builder(KeyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }
}

actual fun createSessionTokenStore(): SessionTokenStore {
    check(AndroidFirebasePlatform.isInitialized()) {
        "AndroidFirebasePlatform nao inicializada - chame initializeAndroidFirebasePlatform(context) em MainActivity.onCreate."
    }
    return AndroidSessionTokenStore(AndroidFirebasePlatform.applicationContext)
}
