# 禁止 R8 进行代码优化（大幅提速 CI 构建，APK 体积略微增大，但不影响功能）
-dontoptimize

# ========== Compose ==========
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ========== Kotlin ==========
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# ========== ViewModel ==========
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ========== kotlinx.coroutines ==========
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ========== 压缩库 ==========
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keep class com.github.junrar.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.junrar.**
-dontwarn org.slf4j.**

# ========== zip4j ==========
-keep class net.lingala.zip4j.** { *; }
-keep class net.lingala.zip4j.model.** { *; }
-dontwarn net.lingala.zip4j.**

# ========== SMBJ ==========
-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**

# ========== apksig ==========
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-dontwarn org.conscrypt.**

# ========== Google Smali / dexlib2 (新版) ==========
-keep class com.android.tools.smali.** { *; }
-dontwarn com.android.tools.smali.**
-dontwarn javax.annotation.**

# ========== Media3 ==========
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ========== FTP Server / MINA ==========
-keep class org.apache.ftpserver.** { *; }
-keep class org.apache.mina.** { *; }
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

# ========== Shizuku ==========
-keep class dev.rikka.shizuku.** { *; }
-dontwarn dev.rikka.shizuku.**

# ========== EL 表达式语言 ==========
-dontwarn javax.el.**
-dontwarn net.engio.mbassy.**

# ========== 其他 ==========
-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn aQute.**
-dontwarn org.apache.harmony.**

# ========== ARSCLib (核心) ==========
# 保留 ARSCLib 的全部代码（它是 APK 解析的核心）
-keep class com.reandroid.** { *; }
-dontwarn com.reandroid.**

# 保留 Android XML 解析相关（ARSCLib 序列化依赖）
-keep class org.xmlpull.v1.** { *; }
-dontwarn org.xmlpull.v1.**