package com.ertyu.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.ertyu.model.ApkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ApkInspector {

    suspend fun inspect(context: Context, apkFile: File): Result<ApkInfo> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pm = context.packageManager
                val flags = PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS

                val info = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
                    ?: throw IllegalArgumentException("无法解析 APK")

                val app: ApplicationInfo = info.applicationInfo
                    ?: throw IllegalArgumentException("APK 缺少 application 信息")

                app.sourceDir = apkFile.absolutePath
                app.publicSourceDir = apkFile.absolutePath

                val label = runCatching {
                    pm.getApplicationLabel(app).toString()
                }.getOrDefault(info.packageName)

                val icon: Bitmap? = runCatching {
                    drawableToBitmap(pm.getApplicationIcon(app))
                }.getOrNull()

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }

                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    app.minSdkVersion
                } else {
                    0
                }

                val size = apkFile.length()

                val signature = ApkSignatureInspector
                    .inspect(context, apkFile)
                    .getOrNull()

                ApkInfo(
                    filePath = apkFile.absolutePath,
                    fileName = apkFile.name,
                    fileSize = size,
                    fileSizeText = formatSize(size),
                    label = label,
                    packageName = info.packageName,
                    versionName = info.versionName ?: "—",
                    versionCode = versionCode,
                    minSdk = minSdk,
                    targetSdk = app.targetSdkVersion,
                    permissions = info.requestedPermissions?.toList() ?: emptyList(),
                    activityCount = info.activities?.size ?: 0,
                    serviceCount = info.services?.size ?: 0,
                    receiverCount = info.receivers?.size ?: 0,
                    providerCount = info.providers?.size ?: 0,
                    isDebug = (app.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0,
                    icon = icon,
                    signature = signature
                )
            }
        }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bmp
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}