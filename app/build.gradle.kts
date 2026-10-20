plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.grandmatv.player"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.grandmatv.player"
        minSdk = 29
        targetSdk = 32
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
