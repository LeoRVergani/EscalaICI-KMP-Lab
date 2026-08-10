import com.android.build.api.dsl.ApplicationExtension
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
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.0")
            implementation("androidx.lifecycle:lifecycle-process:2.8.7")
            implementation("org.apache.poi:poi:5.2.5")
            implementation("org.apache.poi:poi-ooxml:5.2.5")
            // Engine HTTP Android/JVM - sockets reais, sem equivalente em navegador (FASE 17B.1).
            implementation(libs.ktor.client.cio)
        }

        wasmJsMain.dependencies {
            // Engine HTTP para navegador - usa fetch() nativo por baixo dos panos.
            // CIO (sockets) nao funciona em browser real: lanca
            // "Node.js net module is not available" ao tentar enviar qualquer
            // requisicao (achado da FASE 17B, corrigido na FASE 17B.1).
            implementation(libs.ktor.client.js)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

val firebaseConfigOutputDir = layout.buildDirectory.dir("generated/firebaseConfig")

val generateFirebaseConfig = tasks.register("generateFirebaseConfig") {
    val propertiesFile = rootProject.file("local.firebase.properties")
    // `inputs.file(...).optional()` ainda falha a validacao do Gradle quando
    // o arquivo esta totalmente ausente ("Property specifies file which
    // doesn't exist") - achado da FASE 17B, corrigido na FASE 17B.1.
    // `inputs.files(...)` (uma FileCollection, nao um unico InputFile) nao
    // exige que os arquivos existam - o `doLast` abaixo ja trata a ausencia
    // via `propertiesFile.exists()`.
    inputs.files(propertiesFile).withPropertyName("localFirebaseProperties")
    outputs.dir(firebaseConfigOutputDir)

    doLast {
        val properties = Properties()
        if (propertiesFile.exists()) {
            propertiesFile.inputStream().use { properties.load(it) }
        }
        fun prop(key: String, default: String = "") = properties.getProperty(key, default)

        // FASE 17B - fail-fast: uma build com `firebase.environment=STAGING`
        // (Android ou Web, local ou CI) nunca pode compilar silenciosamente
        // com configuracao vazia/errada - risco identificado na auditoria
        // 17B (build Web anterior compilava, mas nao permitia logar de
        // verdade). So valida presenca/projectId, nunca imprime apiKey.
        val environmentValue = prop("firebase.environment", "LOCAL_EMULATOR")
        if (environmentValue == "STAGING") {
            val requiredKeys = listOf(
                "firebase.projectId", "firebase.apiKey", "firebase.authDomain",
                "firebase.appId", "firebase.messagingSenderId",
            )
            val missing = requiredKeys.filter { prop(it).isBlank() }
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "generateFirebaseConfig: firebase.environment=STAGING mas os seguintes campos " +
                        "estao vazios em local.firebase.properties (ou no segredo FIREBASE_PROPERTIES do CI): " +
                        "${missing.joinToString(", ")}. Uma build destinada a staging nao pode compilar " +
                        "silenciosamente com configuracao vazia.",
                )
            }
            val projectId = prop("firebase.projectId")
            val expectedStagingProjectId = "escala-ici-staging"
            if (projectId != expectedStagingProjectId) {
                throw GradleException(
                    "generateFirebaseConfig: firebase.environment=STAGING mas firebase.projectId='$projectId' " +
                        "difere do projeto staging esperado ('$expectedStagingProjectId'). Corrija " +
                        "local.firebase.properties ou o segredo FIREBASE_PROPERTIES do CI.",
                )
            }
        }

        val packageDir = firebaseConfigOutputDir.get()
            .dir("br/com/leorvergani/escalaici/kmp/lab/firebase").asFile
        packageDir.mkdirs()
        packageDir.resolve("GeneratedFirebaseConfig.kt").writeText(
            """
            |package br.com.leorvergani.escalaici.kmp.lab.firebase
            |
            |// GERADO AUTOMATICAMENTE pela task Gradle `generateFirebaseConfig` a partir de
            |// local.firebase.properties (gitignored, config publica apenas - nunca
            |// credenciais de teste, que vivem em local.firebase.test.properties e nunca
            |// sao lidas por esta task). Nao editar a mao; nao versionado.
            |internal val generatedFirebaseConfig = EscalaIciFirebaseConfig(
            |    environment = FirebaseEnvironment.${prop("firebase.environment", "LOCAL_EMULATOR")},
            |    projectId = "${prop("firebase.projectId")}",
            |    apiKey = "${prop("firebase.apiKey")}",
            |    authDomain = "${prop("firebase.authDomain")}",
            |    appId = "${prop("firebase.appId")}",
            |    storageBucket = "${prop("firebase.storageBucket")}",
            |    messagingSenderId = "${prop("firebase.messagingSenderId")}",
            |    emulatorProjectId = "${prop("firebase.emulator.projectId", "demo-escalaici-kmp")}",
            |    emulatorAuthHost = "${prop("firebase.emulator.authHost", "127.0.0.1")}",
            |    emulatorAuthPort = ${prop("firebase.emulator.authPort", "9099")},
            |    emulatorFirestoreHost = "${prop("firebase.emulator.firestoreHost", "127.0.0.1")}",
            |    emulatorFirestorePort = ${prop("firebase.emulator.firestorePort", "8080")},
            |)
            |""".trimMargin()
        )
    }
}

kotlin.sourceSets.getByName("commonMain").kotlin.srcDir(firebaseConfigOutputDir)

tasks.matching { task -> task.name.contains("Kotlin") }.configureEach { dependsOn(generateFirebaseConfig) }

// Repo root para os testes de integracao (androidUnitTest) localizarem
// local.firebase.properties/local.firebase.test.properties - esses testes
// so exercitam rede real quando ESCALAICI_FIREBASE_EMULATOR/_STAGING=true
// (ver firebase/FirebaseIntegrationTest.kt); sem a env var, ficam ignorados
// (Assume) e nao afetam o testDebugUnitTest padrao.
tasks.withType<Test>().configureEach {
    systemProperty("escalaici.repoRoot", rootProject.projectDir.absolutePath)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici.kmp.lab"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.leorvergani.escalaici.kmp.lab"
        minSdk = 28
        targetSdk = 36
        versionCode = 15
        versionName = "0.7.1"
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
        }
        getByName("release") {
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("lab")
            }
        }
    }
}
