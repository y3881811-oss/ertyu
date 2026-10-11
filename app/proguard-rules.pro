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

# ========== 压缩库：保留必要类 ==========
-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keep class com.github.junrar.** { *; }

# ========== 忽略压缩库的可选依赖 ==========
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.junrar.**

# SLF4J 是可选绑定，运行时不需要
-dontwarn org.slf4j.**

# 其他可选依赖
-dontwarn javax.annotation.**
-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn aQute.**
-dontwarn org.apache.harmony.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.**
-dontwarn org.codehaus.mojo.**