plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "de.pquiel.workout"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.pquiel.workout"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        // Fester Schlüssel aus dem CI (GitHub-Secrets), damit jede neue APK als Update über die alte
        // installiert werden kann. Ohne die Umgebungsvariablen bleibt der lokale Debug-Schlüssel.
        val keystore = System.getenv("SIGNING_KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }
        val password = System.getenv("SIGNING_KEYSTORE_PASSWORD")
        if (keystore != null && !password.isNullOrEmpty()) {
            getByName("debug") {
                storeFile = keystore
                storeType = "PKCS12"
                storePassword = password
                keyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "workout"
                keyPassword = password
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Wie das Debug-APK signiert, damit das Release-APK direkt installierbar ist.
            signingConfig = signingConfigs.getByName("debug")
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
