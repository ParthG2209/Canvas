plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.canvas.multitask.service"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        aidl = true
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
    // Shizuku API for ShizukuBinderWrapper and UserService patterns
    implementation(libs.shizuku.api)

    // Hidden API stubs — compile-only so we can reference IActivityTaskManager etc.
    compileOnly(project(":hidden-api-stub"))

    // Coroutines
    implementation(libs.coroutines.core)
}
