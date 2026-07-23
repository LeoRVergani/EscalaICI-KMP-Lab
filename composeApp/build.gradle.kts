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
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.0")
            implementation(libs.androidx.fragment)
            implementation("org.apache.poi:poi:5.2.5")
            implementation("org.apache.poi:poi-ooxml:5.2.5")
            // CIO so funciona em JVM/Native (usa java.net/kotlinx-io de socket real) - nao
            // funciona em navegador. O alvo wasmJs usa ktor-client-js (fetch), ver abaixo.
            implementation(libs.ktor.client.cio)
        }

        wasmJsMain.dependencies {
            // ktor-client-cio tenta usar o modulo "net" do Node.js e falha em um navegador
            // real de verdade ("Node.js net module is not available") - so "funcionava" nos
            // nossos testes automatizados porque o Chromium headless via CDP nunca chegou a
            // exercitar essa chamada de rede antes do MSAL Web existir. ktor-client-js usa
            // fetch/XMLHttpRequest do navegador, o engine correto para este alvo.
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.ktor.client.mock)
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

// Config Firebase Demo: lida de composeApp/google-services.json (Android) ou
// web-config.local.json (Web/Wasm), ambos gitignorados. Como no auth-config,
// nunca quebra build local/CI quando ausente ou preenchido com placeholders.
val googleServicesFile = project.file("google-services.json")
val webConfigFile = rootProject.file("web-config.local.json")

@Suppress("UNCHECKED_CAST")
fun parseJsonMap(file: File): Map<String, Any?>? =
    runCatching {
        if (file.exists()) JsonSlurper().parse(file) as? Map<String, Any?> else null
    }.getOrNull()

val googleServicesConfig = parseJsonMap(googleServicesFile)
val webConfig = parseJsonMap(webConfigFile)

@Suppress("UNCHECKED_CAST")
fun firebaseProjectIdFromGoogleServices(config: Map<String, Any?>?): String =
    realValue((config?.get("project_info") as? Map<String, Any?>)?.get("project_id")) ?: ""

@Suppress("UNCHECKED_CAST")
fun firebaseApiKeyFromGoogleServices(config: Map<String, Any?>?): String {
    val clients = config?.get("client") as? List<Map<String, Any?>>
    val firstClient = clients?.firstOrNull()
    val keys = firstClient?.get("api_key") as? List<Map<String, Any?>>
    return realValue(keys?.firstOrNull()?.get("current_key")) ?: ""
}

@Suppress("UNCHECKED_CAST")
fun firebaseProjectIdFromWebConfig(config: Map<String, Any?>?): String {
    val firebase = config?.get("firebase") as? Map<String, Any?>
    val demoFirebase = config?.get("demo_firebase") as? Map<String, Any?>
    return realValue(demoFirebase?.get("project_id"))
        ?: realValue(firebase?.get("project_id"))
        ?: realValue(config?.get("firebase_project_id"))
        ?: ""
}

@Suppress("UNCHECKED_CAST")
fun firebaseApiKeyFromWebConfig(config: Map<String, Any?>?): String {
    val firebase = config?.get("firebase") as? Map<String, Any?>
    val demoFirebase = config?.get("demo_firebase") as? Map<String, Any?>
    return realValue(demoFirebase?.get("api_key"))
        ?: realValue(demoFirebase?.get("current_key"))
        ?: realValue(firebase?.get("api_key"))
        ?: realValue(firebase?.get("current_key"))
        ?: realValue(config?.get("firebase_api_key"))
        ?: ""
}

fun buildConfigString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

@Suppress("UNCHECKED_CAST")
fun msalWebScopeFromAuthConfig(config: Map<String, Any?>?): List<String> {
    val web = config?.get("web") as? Map<String, Any?>
    val scopes = web?.get("scope") as? List<Any?>
    return scopes?.mapNotNull { realValue(it) }.orEmpty()
}

val demoFirebaseAndroidProjectId = firebaseProjectIdFromGoogleServices(googleServicesConfig)
val demoFirebaseWebProjectId = firebaseProjectIdFromWebConfig(webConfig)
    .ifBlank { demoFirebaseAndroidProjectId }

@Suppress("UNCHECKED_CAST")
val msalWebConfig = authConfig?.get("web") as? Map<String, Any?>
val msalWebTenantId = realValue(authConfig?.get("tenant_id")) ?: ""
val msalWebClientId = realValue(authConfig?.get("client_id")) ?: ""
val msalWebRedirectUriLocal = realValue(msalWebConfig?.get("redirect_uri_local")) ?: ""
val msalWebScopes = msalWebScopeFromAuthConfig(authConfig)

val generatedWasmFirebaseConfigDir = layout.buildDirectory.dir("generated/source/demoFirebaseConfig/wasmJsMain")
val generateWasmFirebaseConfig by tasks.registering {
    val outputFile = generatedWasmFirebaseConfigDir.map {
        it.file("br/com/leorvergani/escalaici/source/DemoFirebaseConfig.wasmJs.generated.kt")
    }
    outputs.file(outputFile)
    inputs.property("demoFirebaseWebProjectId", demoFirebaseWebProjectId)

    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package br.com.leorvergani.escalaici.source

            actual fun platformDemoFirebaseConfig(): DemoFirebaseConfig =
                DemoFirebaseConfig(
                    projectId = ${buildConfigString(demoFirebaseWebProjectId)}
                )
            """.trimIndent() + "\n"
        )
    }
}

val generatedWasmMsalWebConfigDir = layout.buildDirectory.dir("generated/source/msalWebConfig/wasmJsMain")
val generateWasmMsalWebConfig by tasks.registering {
    val outputFile = generatedWasmMsalWebConfigDir.map {
        it.file("br/com/leorvergani/escalaici/auth/MsalWebConfig.wasmJs.generated.kt")
    }
    outputs.file(outputFile)
    inputs.property("msalWebTenantId", msalWebTenantId)
    inputs.property("msalWebClientId", msalWebClientId)
    inputs.property("msalWebRedirectUriLocal", msalWebRedirectUriLocal)
    inputs.property("msalWebScopes", msalWebScopes)

    doLast {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            package br.com.leorvergani.escalaici.auth

            val platformMsalWebConfig: MsalWebConfig = MsalWebConfig(
                tenantId = ${buildConfigString(msalWebTenantId)},
                clientId = ${buildConfigString(msalWebClientId)},
                redirectUri = ${buildConfigString(msalWebRedirectUriLocal)},
                scopes = listOf(${msalWebScopes.joinToString(", ") { buildConfigString(it) }})
            )
            """.trimIndent() + "\n"
        )
    }
}

kotlin.sourceSets.named("wasmJsMain") {
    kotlin.srcDir(generatedWasmFirebaseConfigDir)
    kotlin.srcDir(generatedWasmMsalWebConfigDir)
}

tasks.matching { it.name.contains("compileKotlinWasmJs", ignoreCase = true) }.configureEach {
    dependsOn(generateWasmFirebaseConfig)
    dependsOn(generateWasmMsalWebConfig)
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici"
    compileSdk = 36

    defaultConfig {
        applicationId = androidApplicationId
        minSdk = 28
        targetSdk = 36
        versionCode = 29
        versionName = "0.7.15"

        buildConfigField("String", "MSAL_TENANT_ID", "\"$msalTenantId\"")
        buildConfigField("String", "MSAL_CLIENT_ID", "\"$msalClientId\"")
        buildConfigField("String", "MSAL_REDIRECT_URI_DEBUG", "\"$msalRedirectUriDebug\"")
        buildConfigField("String", "MSAL_REDIRECT_URI_RELEASE", "\"$msalRedirectUriRelease\"")
        buildConfigField("String", "DEMO_FIREBASE_PROJECT_ID", buildConfigString(demoFirebaseAndroidProjectId))
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
