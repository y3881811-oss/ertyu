plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val storePass: String = System.getenv("KEYSTORE_PASSWORD") ?: "mtfm123456"
val aliasName: String = System.getenv("KEY_ALIAS") ?: "mtfm"
val keyPass: String = System.getenv("KEY_PASSWORD") ?: "mtfm123456"

android {
    namespace = "com.ertyu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ertyu"
        minSdk = 26
        targetSdk = 28
        versionCode = 1
        versionName = "0.1.0"

        // 只保留 32 位 + 64 位 ARM，去掉 x86_64（模拟器用，普通手机不需要）
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    signingConfigs {
        create("mtfm") {
            storeFile = rootProject.file("keystore/mtfm.jks")
            storePassword = storePass
            keyAlias = aliasName
            keyPassword = keyPass
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("mtfm")
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("mtfm")
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/NOTICE.md",
                "META-INF/INDEX.LIST",
                "META-INF/*.kotlin_module",
                "META-INF/versions/**",
                "kotlin/**",
                "DebugProbesKt.bin",
                "kotlin-tooling-metadata.json"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    androidResources {
        localeFilters += listOf("zh", "en")
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
    // 已从 extended 换成 core
    implementation(libs.androidx.material.icons.core)

    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.zstd.jni)
    implementation(libs.junrar)
}