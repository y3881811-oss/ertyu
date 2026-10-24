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

# ========== apksig ==========
-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**

# ========== baksmali / dexlib2 ==========
-keep class org.jf.** { *; }
-keep class org.smali.** { *; }
-dontwarn org.jf.**
-dontwarn org.smali.**
-dontwarn javax.annotation.**

# ========== Media3 ==========
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ========== FTP Server ==========
-keep class org.apache.ftpserver.** { *; }
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**
-keep class org.slf4j.** { *; }

# ========== 其他 ==========
-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn aQute.**
-dontwarn org.apache.harmony.**