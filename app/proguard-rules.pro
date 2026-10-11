# Compose 保持规则
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Kotlin 元数据
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# 保留 ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }

# kotlinx.coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}