plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.beingolea.recordatorios"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.beingolea.recordatorios"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.2"
        buildConfigField("String", "API_BASE_URL", "\"https://recordatorio-familiar-beingolea.saludos-familia.workers.dev\"")
        buildConfigField("String", "ONESIGNAL_APP_ID", "\"c31f4d2e-5002-450f-aaea-a2e1a4570094\"")
    }

    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.onesignal:OneSignal:[5.6.1, 5.99.99]")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")
}
