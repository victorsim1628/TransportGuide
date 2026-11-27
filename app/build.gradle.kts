plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.transportguide.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.transportguide.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 9
        versionName = "1.1.0"

        // Only the 64-bit ARM build of the translation engine (every phone since about 2017);
        // the armv7/x86 copies added ~46 MB.
        ndk { abiFilters += "arm64-v8a" }
    }

    // A fixed debug key (standard "android" passwords, not a secret) so every build, here or on
    // GitHub, has the same signature and new versions install over old ones.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("com.google.mlkit:translate:17.0.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
