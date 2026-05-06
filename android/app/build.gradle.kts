plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Load sensitive values from local.properties (not committed to git)
val localProps = java.util.Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun localProp(key: String, default: String = ""): String =
    localProps.getProperty(key, default)

android {
    namespace = "com.solaria.app"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.solaria.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // All sensitive values loaded from local.properties — see README for setup
        buildConfigField("String", "BASE_URL", "\"${localProp("BASE_URL", "http://10.0.2.2:8788/")}\"")
        buildConfigField("String", "API_KEY", "\"${localProp("API_KEY")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${localProp("GEMINI_API_KEY")}\"")
        buildConfigField("String", "BITREFILL_API_KEY", "\"${localProp("BITREFILL_API_KEY")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)
    implementation(libs.material)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // ExoPlayer / Media3 (Commented out - can be easily re-integrated later for voice features)
    // implementation(libs.media3.exoplayer)
    // implementation(libs.media3.ui)
    // implementation(libs.media3.datasource.okhttp)

    // Coroutines
    implementation(libs.coroutines.android)

    // Coil
    implementation(libs.coil.compose)

    // Solana Ed25519 crypto
    implementation(libs.eddsa)

    // Encrypted key storage
    implementation(libs.security.crypto)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // WorkManager
    implementation(libs.workmanager)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    // Google Generative AI (Gemini)
    implementation(libs.generativeai)
}
