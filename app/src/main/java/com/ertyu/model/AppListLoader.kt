package com.ertyu.engine

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import com.ertyu.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AppListLoader {

    private var cache: List<InstalledApp>? = null

    suspend fun load(
        context: Context,
        forceRefresh: Boolean = false
    ): List<InstalledApp> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            cache?.let { return@withContext it }
        }

        val pm = context.packageManager
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val result = ArrayList<InstalledApp>(packages.size)

        for (ai in packages) {
            try {
                val pkg = ai.packageName
                val info = runCatching {
                    pm.getPackageInfo(pkg, 0)
                }.getOrNull() ?: continue

                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val label = runCatching {
                    pm.getApplicationLabel(ai).toString()
                }.getOrDefault(pkg)

                val icon: Bitmap? = runCatching {
                    drawableToBitmap(pm.getApplicationIcon(ai))
                }.getOrNull()

                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    info.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }

                val apkPath = ai.sourceDir ?: ""
                val size = runCatching {
                    File(apkPath).length()
                }.getOrDefault(0L)

                result.add(
                    InstalledApp(
                        label = label,
                        packageName = pkg,
                        versionName = info.versionName ?: "—",
                        versionCode = versionCode,
                        isSystem = isSystem,
                        apkPath = apkPath,
                        size = size,
                        sizeText = formatSize(size),
                        icon = icon
                    )
                )
            } catch (_: Exception) {
                // 忽略单个应用加载失败
            }
        }

        result.sortWith(compareBy({ it.isSystem }, { it.label.lowercase() }))
        cache = result
        result
    }

    fun invalidate() {
        cache = null
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