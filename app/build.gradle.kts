plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0+ 已内置 Kotlin 支持
    alias(libs.plugins.kotlin.compose)
}

// ================== 签名参数配置 ==================
val storePass: String = System.getenv("KEYSTORE_PASSWORD") ?: "mtfm123456"
val aliasName: String = System.getenv("KEY_ALIAS") ?: "mtfm"
val keyPass: String = System.getenv("KEY_PASSWORD") ?: "mtfm123456"

android {
    namespace = "com.ertyu"
    // 配合 Compose 2026.08.00 和 AGP 9.2，compileSdk 必须为 37
    compileSdk = 37

    defaultConfig {
        applicationId = "com.ertyu"
        minSdk = 26
        targetSdk = 28
        
        // 动态读取 GitHub Actions 传入的版本号
        versionCode = (project.findProperty("appVersionCode") as String?)?.toInt() ?: 1
        versionName = (project.findProperty("appVersionName") as String?) ?: "0.1.0"

        ndk {
            // 👇 只保留 64 位（arm64-v8a），去掉了 32 位（armeabi-v7a）
            abiFilters += listOf("arm64-v8a")
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
            isMinifyEnabled = false
        }
        getByName("release") {
            // 这是 Release 配置：开启混淆和资源压缩
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
        disable += setOf("LintError")
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// ================== AGP 9 内置 Kotlin 配置 ==================
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// ================== Compose 编译器优化 ==================
composeCompiler {
    // 开启强跳过模式，大幅优化快速滑动卡顿
    featureFlags.add(androidx.compose.compiler.plugins.kotlin.ComposeFeatureFlag.StrongSkipping)
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

    // ===== 网络与服务器 =====
    implementation(libs.ftpserver.core)
    implementation(libs.smbj)

    // ===== 权限管理 =====
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    // ===== 媒体播放 (Media3) =====
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // ===== APK 与 DEX 处理工具 =====
    implementation(libs.arsc.lib)         // 替代 apk-parser 和 APKEditor
    implementation(libs.smali.baksmali)   // 替代旧 baksmali
    implementation(libs.smali.dexlib2)    // 辅助 DEX 底层 API
}