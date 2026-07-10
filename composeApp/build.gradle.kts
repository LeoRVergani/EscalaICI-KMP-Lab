import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
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
        }

        androidMain.dependencies {
            implementation("androidx.activity:activity-compose:1.12.0")
            implementation("org.apache.poi:poi:5.2.5")
            implementation("org.apache.poi:poi-ooxml:5.2.5")
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

extensions.configure<ApplicationExtension>("android") {
    namespace = "br.com.leorvergani.escalaici.kmp.lab"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.leorvergani.escalaici.kmp.lab"
        minSdk = 28
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1-lab"
    }
}
