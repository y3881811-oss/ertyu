plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.ertyu"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ertyu"
        minSdk = 26 // 因为 zip4j 和 commons-compress 的要求
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // 防止 smbj 等库的冲突
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    // ===== AndroidX 基础 =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // ===== Compose =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // ===== 压缩与解压 =====
    implementation(libs.commons.compress)
    implementation(libs.xz)
    implementation(libs.zstd.jni)
    implementation(libs.junrar)
    implementation(libs.zip4j)

    // ===== 签名与安装 =====
    implementation(libs.apksig)

    // ===== 媒体播放 (Media3) =====
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // ===== 网络与服务器 =====
    implementation(libs.ftpserver.core)
    implementation(libs.smbj)

    // ===== 权限管理 =====
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // ===== APK 与 DEX 处理工具 (已替换废弃库) =====
    implementation(libs.apkeditor)        // 替代 apk-parser
    implementation(libs.smali.baksmali)   // 替代旧 baksmali
    implementation(libs.smali.dexlib2)    // 辅助 DEX 底层 API
}