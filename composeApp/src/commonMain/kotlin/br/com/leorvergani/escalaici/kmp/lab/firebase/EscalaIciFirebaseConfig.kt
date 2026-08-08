package br.com.leorvergani.escalaici.kmp.lab.firebase

/**
 * Ambiente Firebase alvo. `PRODUCTION` existe apenas para permitir a
 * configuracao futura - o Escala-ICI (fonte de verdade) ainda nao define um
 * projeto de producao (so LOCAL_EMULATOR/staging existem hoje, ver
 * FASE-15-FIREBASE-UNIFICADO.md).
 */
enum class FirebaseEnvironment {
    LOCAL_EMULATOR,
    STAGING,
    PRODUCTION,
}

/**
 * Config publica do Firebase (apiKey/projectId/etc nao sao segredo) usada
 * pelos clientes REST de Auth/Firestore. Gerada em build-time a partir de
 * `local.firebase.properties` (gitignored, nunca contem credenciais de
 * teste) por `generateFirebaseConfig` (composeApp/build.gradle.kts) -
 * `GeneratedFirebaseConfig.kt` fornece a instancia real via
 * [currentFirebaseConfig].
 */
data class EscalaIciFirebaseConfig(
    val environment: FirebaseEnvironment,
    val projectId: String,
    val apiKey: String,
    val authDomain: String,
    val appId: String,
    val storageBucket: String,
    val messagingSenderId: String,
    val emulatorProjectId: String,
    val emulatorAuthHost: String,
    val emulatorAuthPort: Int,
    val emulatorFirestoreHost: String,
    val emulatorFirestorePort: Int,
) {
    val usesEmulator: Boolean get() = environment == FirebaseEnvironment.LOCAL_EMULATOR

    val effectiveProjectId: String get() = if (usesEmulator) emulatorProjectId else projectId

    val identityToolkitBaseUrl: String
        get() = if (usesEmulator) {
            "http://$emulatorAuthHost:$emulatorAuthPort/identitytoolkit.googleapis.com/v1"
        } else {
            "https://identitytoolkit.googleapis.com/v1"
        }

    val secureTokenBaseUrl: String
        get() = if (usesEmulator) {
            "http://$emulatorAuthHost:$emulatorAuthPort/securetoken.googleapis.com/v1"
        } else {
            "https://securetoken.googleapis.com/v1"
        }

    val firestoreBaseUrl: String
        get() = if (usesEmulator) {
            "http://$emulatorFirestoreHost:$emulatorFirestorePort/v1/projects/$effectiveProjectId/databases/(default)/documents"
        } else {
            "https://firestore.googleapis.com/v1/projects/$effectiveProjectId/databases/(default)/documents"
        }

    /** Chave de API exigida nos endpoints REST do Identity Toolkit/Secure Token, mesmo contra o emulador (aceita qualquer valor). */
    val effectiveApiKey: String get() = apiKey.ifBlank { "demo-api-key" }

    /**
     * Nome de recurso completo de um documento (`projects/{p}/databases/(default)/documents/{collection}/{id}`),
     * exigido pelo corpo de `:commit` - e sempre este formato logico, mesmo contra o Emulator (nunca inclui host/porta/`v1`).
     */
    fun documentName(collection: String, documentId: String): String =
        "projects/$effectiveProjectId/databases/(default)/documents/$collection/$documentId"
}

fun currentFirebaseConfig(): EscalaIciFirebaseConfig = generatedFirebaseConfig
