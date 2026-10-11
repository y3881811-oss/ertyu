# Compose
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Kotlin
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# kotlinx.coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# 压缩库：不混淆，避免 native 方法被裁剪
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keep class com.github.junrar.** { *; }

-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.junrar.**
-dontwarn javax.annotation.**
-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn aQute.**