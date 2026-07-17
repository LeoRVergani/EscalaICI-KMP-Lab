import com.android.build.api.dsl.ApplicationExtension
import groovy.json.JsonSlurper
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
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
    }
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

// Config MSAL: lida em tempo de build de auth-config.json (raiz do projeto, real,
// gitignorado — ver auth-config.example.json). Nunca falha se o arquivo nao existir
// ou estiver com placeholders: os buildConfigField sempre sao gerados, so mudam de
// valor. Ver docs/spec/53 secao 5 para a justificativa completa desta abordagem.
val authConfigFile = rootProject.file("auth-config.json")

@Suppress("UNCHECKED_CAST")
val authConfig: Map<String, Any?>? =
    if (authConfigFile.exists()) JsonSlurper().parse(authConfigFile) as? Map<String, Any?> else null

@Suppress("UNCHECKED_CAST")
val androidAuthConfig: Map<String, Any?>? = authConfig?.get("android") as? Map<String, Any?>

fun realValue(value: Any?): String? =
    (value as? String)?.takeUnless { it.isBlank() || it.startsWith("<") }

val msalTenantId = realValue(authConfig?.get("tenant_id")) ?: ""
val msalClientId = realValue(authConfig?.get("client_id")) ?: ""
val msalRedirectUriDebug = realValue(androidAuthConfig?.get("redirect_uri_debug")) ?: ""
val msalRedirectUriRelease = realValue(androidAuthConfig?.get("redirect_uri_release")) ?: ""
val msalSignatureHashDebug = realValue(androidAuthConfig?.get("signature_hash_debug")) ?: ""
val msalSignatureHashRelease = realValue(androidAuthConfig?.get("signature_hash_release")) ?: ""

val msalConfigured = listOf(msalTenantId, msalClientId, msalRedirectUriDebug, msalRedirectUriRelease)
    .all { it.isNotBlank() }

// MSAL exige o hash de assinatura codificado como URL dentro do redirect_uri/manifest
// (RFC 3986: '+' -> %2B, '/' -> %2F, '=' -> %3D), ver docs/msal/android FAQ de redirect URI.
fun urlEncodeSignatureHash(hash: String): String =
    hash.replace("+", "%2B").replace("/", "%2F").replace("=", "%3D")

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.leorvergani.escalaici"
        minSdk = 28
        targetSdk = 36
        versionCode = 17
        versionName = "0.7.3"

        buildConfigField("boolean", "MSAL_CONFIGURED", msalConfigured.toString())
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
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("lab")
            }
            manifestPlaceholders["msalSignatureHashPath"] =
                "/" + urlEncodeSignatureHash(msalSignatureHashDebug.ifBlank { "NOT_CONFIGURED" })
        }
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("lab")
            }
            manifestPlaceholders["msalSignatureHashPath"] =
                "/" + urlEncodeSignatureHash(msalSignatureHashRelease.ifBlank { "NOT_CONFIGURED" })
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
