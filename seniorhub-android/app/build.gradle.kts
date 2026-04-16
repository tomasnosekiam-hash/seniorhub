import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
}

/** Čas konfigurace Gradle = při každém assemble nová hodnota — poznáš, že je na tabletu čerstvý APK. */
private val seniorHubBuildStamp: String =
    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

android {
    namespace = "com.seniorhub.os"
    compileSdk = 35

    val localProps = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) f.inputStream().use { load(it) }
    }
    val picovoiceAccessKey: String = localProps.getProperty("picovoice.access.key", "")
    if (picovoiceAccessKey.isBlank()) {
        logger.lifecycle(
            "[seniorhub-android] picovoice.access.key missing in local.properties — " +
                "Matěj Porcupine wake will not start (add key next to settings.gradle.kts and rebuild).",
        )
    }
    // Klíč drž v seniorhub-android/local.properties: gemini.api.key=… (soubor je v .gitignore).
    val geminiApiKey: String = localProps.getProperty("gemini.api.key", "")
    if (geminiApiKey.isBlank()) {
        logger.lifecycle(
            "[seniorhub-android] gemini.api.key missing in local.properties — " +
                "Matěj použije jen heuristiky (přidej klíč z Google AI Studio pro Gemini Flash).",
        )
    }
    // Cloud fallback (když Nano není): výchozí = nejlevnější Flash-Lite v Gemini API (viz pricing).
    // Override v local.properties: gemini.cloud.model=… (např. gemini-2.5-flash pro vyšší kvalitu, gemini-2.5-flash-lite pro levný GA).
    val geminiCloudModel: String = localProps.getProperty(
        "gemini.cloud.model",
        "gemini-3.1-flash-lite-preview",
    )

    defaultConfig {
        applicationId = "com.seniorhub.os"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0-mvp"
        buildConfigField("String", "PICOVOICE_ACCESS_KEY", "\"$picovoiceAccessKey\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "GEMINI_CLOUD_MODEL", "\"$geminiCloudModel\"")
        buildConfigField("String", "BUILD_STAMP", "\"$seniorHubBuildStamp\"")
    }

    buildTypes {
        debug {
            buildConfigField("String", "BUILD_MARK", "\"debug\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BUILD_MARK", "\"release\"")
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3-window-size-class:1.3.1")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-installations-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    /** Matěj — wake word (Porcupine). */
    implementation("ai.picovoice:porcupine-android:3.0.2")

    /** Matěj 2.0 — Gemini Flash (cloud); bez klíče v BuildConfig zůstávají heuristiky. */
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    /** Matěj 2.0 — Gemini Nano on-device (AICore přes ML Kit Prompt API). */
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}
