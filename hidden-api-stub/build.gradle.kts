// Compile-only module: Hidden API stubs for IActivityTaskManager et al.
// These are NEVER packaged into the APK — they exist so Kotlin code can
// reference hidden types without reflection at compile time.

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.canvas.multitask.hidden"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
