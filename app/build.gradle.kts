plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// 从环境变量读取签名信息（由 GitHub Actions 注入）
val keystorePassword: String = System.getenv("KEYSTORE_PASSWORD") ?: "mtfm123456"
val keyAlias: String = System.getenv("KEY_ALIAS") ?: "mtfm"
val keyPassword: String = System.getenv("KEY_PASSWORD") ?: "mtfm123456"

android {
    namespace = "com.ertyu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ertyu"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("mtfm") {
            storeFile = rootProject.file("keystore/mtfm.jks")
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            keyPassword = keyPassword
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("mtfm")
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("mtfm")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
}