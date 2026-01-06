import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.devlosoft.megaposmobile"
    compileSdk = 36

    signingConfigs {
        create("release") {
            storeFile = file("../megapos-release.jks")
            storePassword = "FMvVUoRsSXz3udLo24vF"
            keyAlias = "megapos"
            keyPassword = "FMvVUoRsSXz3udLo24vF"
        }
    }

    defaultConfig {
        applicationId = "com.devlosoft.megaposmobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig field for API base URL (Gateway port 5166)
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5166/pos-api/v1/\"")
        buildConfigField("String", "FEL_API_BASE_URL", "\"http://10.0.2.2:5166/fel-api/v1/\"")

        // Development mode - skips printer connectivity test
        buildConfigField("Boolean", "DEVELOPMENT_MODE", "true")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:5166/pos-api/v1/\"")
            buildConfigField("String", "FEL_API_BASE_URL", "\"http://10.0.2.2:5166/fel-api/v1/\"")
            buildConfigField("Boolean", "DEVELOPMENT_MODE", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // DataStore
    implementation(libs.datastore.preferences)

    // Kotlin Serialization (for type-safe navigation)
    implementation(libs.kotlinx.serialization.json)

    // Coil (async image loading)
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
