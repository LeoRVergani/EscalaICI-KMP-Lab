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

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici.kmp.lab"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.leorvergani.escalaici.kmp.lab"
        minSdk = 28
        targetSdk = 36
        versionCode = 13
        versionName = "0.6.3"
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
