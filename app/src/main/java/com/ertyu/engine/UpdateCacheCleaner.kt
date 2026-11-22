package com.ertyu.engine

import android.content.Context
import java.io.File

/**
 * 更新包缓存清理：避免 cacheDir/updates 里留一堆 15MB 的旧 APK。
 * 保留 24 小时内、且不在安装中的文件；.part 半包一律删掉。
 */
object UpdateCacheCleaner {

    private const val KEEP_MS = 24L * 60 * 60 * 1000

    fun clean(context: Context, keep: File? = null) {
        try {
            val dir = File(context.cacheDir, "updates")
            if (!dir.isDirectory) return
            val cutoff = System.currentTimeMillis() - KEEP_MS
            dir.listFiles()?.forEach { f ->
                if (keep != null && f.absolutePath == keep.absolutePath) return@forEach
                // .part 只在 1 小时没动过时才删，避免删掉可续传的半包
                val partStale = f.name.endsWith(".part") &&
                    f.lastModified() < System.currentTimeMillis() - 60 * 60 * 1000L
                val stale = partStale || f.lastModified() < cutoff
                if (stale) {
                    try { f.delete() } catch (_: Exception) { }
                }
            }
            val left = dir.listFiles()
            if (left.isNullOrEmpty()) dir.delete()
            else {
                val freedBytes = 0L
                AppLogger.d("UpdateCache", "更新缓存剩余 ${left.size} 个文件（保留 ${KEEP_MS / 3600000} 小时）")
            }
        } catch (_: Exception) { }
    }
}
