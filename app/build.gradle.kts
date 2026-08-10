import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pl.diplomat.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "pl.diplomat"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.7"

        val gitHash = runCatching {
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                isIgnoreExitValue = true
            }.standardOutput.asText.get().trim()
        }.getOrElse { "" }.ifBlank { "unknown" }

        buildConfigField("String", "GIT_COMMIT_HASH", "\"$gitHash\"")
        buildConfigField("String", "APK_BUILT_AT", "\"unknown\"")
    }

    val ciKeystoreFile = System.getenv("DIPLOMAT_KEYSTORE_FILE")?.takeIf { it.isNotBlank() }
    val ciKeystorePassword = System.getenv("DIPLOMAT_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
    val ciKeyAlias = System.getenv("DIPLOMAT_KEY_ALIAS")?.takeIf { it.isNotBlank() }
    val ciKeyPassword = System.getenv("DIPLOMAT_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
    val ciSigning = if (
        ciKeystoreFile != null &&
        ciKeystorePassword != null &&
        ciKeyAlias != null &&
        ciKeyPassword != null
    ) {
        signingConfigs.create("ci") {
            storeFile = file(ciKeystoreFile)
            storePassword = ciKeystorePassword
            keyAlias = ciKeyAlias
            keyPassword = ciKeyPassword
        }
    } else {
        null
    }

    check(System.getenv("DIPLOMAT_REQUIRE_CI_SIGNING") != "true" || ciSigning != null) {
        "CI signing is required but the keystore or signing credentials are missing."
    }

    buildTypes {
        debug {
            if (ciSigning != null) {
                signingConfig = ciSigning
            }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (ciSigning != null) {
                signingConfig = ciSigning
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

afterEvaluate {
    listOf("Debug", "Release").forEach { buildType ->
        tasks.named("generate${buildType}BuildConfig").configure {
            doFirst {
                val buildTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.format(Date())
                android.defaultConfig.buildConfigField("String", "APK_BUILT_AT", "\"$buildTime\"")
            }
        }
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":usecase"))
    implementation(project(":infrastructure"))
    implementation(project(":presentation"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
