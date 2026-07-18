import com.android.build.api.dsl.ApplicationExtension
import groovy.json.JsonSlurper
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Base64
import java.util.Properties

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.compose")
    id("com.android.application")
}

kotlin {
    androidTarget()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.cio)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.0")
            implementation("org.apache.poi:poi:5.2.5")
            implementation("org.apache.poi:poi-ooxml:5.2.5")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }

        androidUnitTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val labStoreFile = if (keystorePropertiesFile.exists()) file(keystoreProperties.getProperty("storeFile")) else null
val labStorePassword = if (keystorePropertiesFile.exists()) keystoreProperties.getProperty("storePassword") else null
val labKeyAlias = if (keystorePropertiesFile.exists()) keystoreProperties.getProperty("keyAlias") else null
val labKeyPassword = if (keystorePropertiesFile.exists()) keystoreProperties.getProperty("keyPassword") else null

// Config MSAL: lida em tempo de build de auth-config.json (raiz do projeto, real,
// gitignorado — ver auth-config.example.json). Nunca falha se o arquivo nao existir
// ou estiver com placeholders: os buildConfigField sempre sao gerados, so mudam de
// valor. Ver docs/spec/53 secao 5 para a justificativa completa desta abordagem.
val authConfigFile = rootProject.file("auth-config.json")

@Suppress("UNCHECKED_CAST")
val authConfig: Map<String, Any?>? =
    if (authConfigFile.exists()) JsonSlurper().parse(authConfigFile) as? Map<String, Any?> else null

fun realValue(value: Any?): String? =
    (value as? String)?.takeUnless { it.isBlank() || it.startsWith("<") }

// MSAL exige o hash de assinatura codificado como URL dentro do redirect_uri/manifest
// (RFC 3986: '+' -> %2B, '/' -> %2F, '=' -> %3D), ver docs/msal/android FAQ de redirect URI.
fun urlEncodeSignatureHash(hash: String): String =
    hash.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D")

// Le o certificado direto via API do KeyStore (sem shell out para `keytool`):
// um processo externo receberia a senha como argumento de linha de comando,
// visivel para qualquer usuario local via /proc/<pid>/cmdline enquanto roda.
fun signatureHashFromKeystore(storeFile: File, storePassword: String, keyAlias: String): String? =
    runCatching {
        if (!storeFile.isFile || storePassword.isBlank() || keyAlias.isBlank()) return null

        val password = storePassword.toCharArray()
        val keyStore = KeyStore.getInstance(storeFile, password)
        val certificate = keyStore.getCertificate(keyAlias) ?: return null

        val digest = MessageDigest.getInstance("SHA-1").digest(certificate.encoded)
        Base64.getEncoder().encodeToString(digest)
    }.getOrNull()

fun redirectUriForSignatureHash(applicationId: String, signatureHash: String?): String =
    signatureHash
        ?.takeIf { it.isNotBlank() }
        ?.let { "msauth://$applicationId/${urlEncodeSignatureHash(it)}" }
        ?: ""

val androidApplicationId = "br.com.leorvergani.escalaici"
val defaultDebugStoreFile = File(System.getProperty("user.home"), ".android/debug.keystore")
val realSignatureHashDebug =
    if (keystorePropertiesFile.exists()) {
        signatureHashFromKeystore(labStoreFile!!, labStorePassword.orEmpty(), labKeyAlias.orEmpty())
    } else {
        signatureHashFromKeystore(defaultDebugStoreFile, "android", "androiddebugkey")
    }
val realSignatureHashRelease =
    if (keystorePropertiesFile.exists()) {
        signatureHashFromKeystore(labStoreFile!!, labStorePassword.orEmpty(), labKeyAlias.orEmpty())
    } else {
        null
    }

val msalTenantId = realValue(authConfig?.get("tenant_id")) ?: ""
val msalClientId = realValue(authConfig?.get("client_id")) ?: ""
val msalRedirectUriDebug = redirectUriForSignatureHash(androidApplicationId, realSignatureHashDebug)
val msalRedirectUriRelease = redirectUriForSignatureHash(androidApplicationId, realSignatureHashRelease)

// Cada variante so exige o proprio redirect: sem keystore.properties (dev sem
// keystore de release), o debug ainda deve poder usar o login corporativo com
// o hash do debug.keystore padrao, mesmo que o release fique NOT_CONFIGURED.
val msalBaseConfigured = msalTenantId.isNotBlank() && msalClientId.isNotBlank()
val msalConfiguredDebug = msalBaseConfigured && msalRedirectUriDebug.isNotBlank()
val msalConfiguredRelease = msalBaseConfigured && msalRedirectUriRelease.isNotBlank()

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici"
    compileSdk = 36

    defaultConfig {
        applicationId = androidApplicationId
        minSdk = 28
        targetSdk = 36
        versionCode = 21
        versionName = "0.7.7"

        buildConfigField("String", "MSAL_TENANT_ID", "\"$msalTenantId\"")
        buildConfigField("String", "MSAL_CLIENT_ID", "\"$msalClientId\"")
        buildConfigField("String", "MSAL_REDIRECT_URI_DEBUG", "\"$msalRedirectUriDebug\"")
        buildConfigField("String", "MSAL_REDIRECT_URI_RELEASE", "\"$msalRedirectUriRelease\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Sem isso, compileDebugJavaWithJavac (agora com fonte real, o BuildConfig.java
    // gerado pelo bloco acima) roda em Java 11 por padrao do AGP, inconsistente com
    // o alvo JVM 21 do Kotlin — so aparecia agora porque antes nao havia fonte Java.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("lab") {
                storeFile = labStoreFile
                storePassword = labStorePassword
                keyAlias = labKeyAlias
                keyPassword = labKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("lab")
            }
            buildConfigField("boolean", "MSAL_CONFIGURED", msalConfiguredDebug.toString())
            manifestPlaceholders["msalSignatureHashPath"] =
                "/" + realSignatureHashDebug.orEmpty().ifBlank { "NOT_CONFIGURED" }
        }
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("lab")
            }
            buildConfigField("boolean", "MSAL_CONFIGURED", msalConfiguredRelease.toString())
            manifestPlaceholders["msalSignatureHashPath"] =
                "/" + realSignatureHashRelease.orEmpty().ifBlank { "NOT_CONFIGURED" }
        }
    }
}

// A lambda de exclude() do MSAL so existe no DependencyHandler classico, nao no
// KotlinDependencyHandler usado dentro de kotlin { sourceSets { androidMain.dependencies {} } }
// (ver docs/spec/53) — por isso a dependencia entra aqui, na configuracao
// "androidMainImplementation" gerada pelo plugin KMP para o source set androidMain.
dependencies {
    // GAV em string (nao o acessor libs.msal direto): MinimalExternalModuleDependency
    // do catalogo de versoes e imutavel e nao aceita exclude() no Kotlin DSL.
    "androidMainImplementation"("com.microsoft.identity.client:msal:${libs.versions.msal.get()}") {
        exclude(group = "io.opentelemetry", module = "opentelemetry-bom")
        exclude(group = "com.microsoft.device.display", module = "display-mask")
    }
}
