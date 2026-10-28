-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keep class org.apache.commons.compress.** { *; }
-keep class org.tukaani.xz.** { *; }
-keep class com.github.luben.zstd.** { *; }
-keep class com.github.junrar.** { *; }
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.github.junrar.**
-dontwarn org.slf4j.**

-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**
-keep class net.dongliu.apk.parser.** { *; }
-dontwarn net.dongliu.apk.parser.**
-dontwarn org.xmlpull.v1.**

-keep class com.android.apksig.** { *; }
-dontwarn com.android.apksig.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-keep class org.jf.** { *; }
-keep class org.smali.** { *; }
-dontwarn org.jf.**
-dontwarn org.smali.**
-dontwarn javax.annotation.**

-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

-keep class org.apache.ftpserver.** { *; }
-keep class org.apache.mina.** { *; }
-dontwarn org.apache.ftpserver.**
-dontwarn org.apache.mina.**
-keepclassmembers class * implements org.apache.mina.core.service.IoProcessor {
    public <init>(java.util.concurrent.ExecutorService);
    public <init>(java.util.concurrent.Executor);
    public <init>();
}

-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn com.hierynomus.**
-dontwarn org.bouncycastle.**

-dontwarn java.awt.**
-dontwarn org.osgi.**
-dontwarn aQute.**
-dontwarn org.apache.harmony.**