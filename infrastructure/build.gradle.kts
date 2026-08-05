plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pl.diplomat.infrastructure"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":usecase"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
