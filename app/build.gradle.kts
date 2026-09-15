plugins {
    id("com.android.application")
}

android {
    namespace = "com.skn.countdown"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.skn.countdown"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.19.0")
}
