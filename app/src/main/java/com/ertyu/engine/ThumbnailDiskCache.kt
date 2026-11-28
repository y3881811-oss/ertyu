package com.ertyu.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

/**
 * 缩略图磁盘缓存：冷启动不用重新解压/解码。
 * 键包含路径 + 大小 + 修改时间，文件改动后自动失效。
 */
object ThumbnailDiskCache {

    private const val MAX_BYTES = 64L * 1024 * 1024
    private var dir: File? = null

    fun init(context: Context) {
        try {
            dir = File(context.cacheDir, "thumbs").apply { mkdirs() }
            trim()
        } catch (_: Exception) { }
    }

    private fun key(path: String): String? {
        return try {
            val f = File(path)
            val raw = "${f.absolutePath}|${f.length()}|${f.lastModified()}"
            MessageDigest.getInstance("SHA-1").digest(raw.toByteArray())
                .joinToString("") { "%02x".format(it) } + ".png"
        } catch (_: Exception) {
            null
        }
    }

    fun get(path: String): Bitmap? {
        if (com.ertyu.engine.VaultGuard.isVaultPath(path)) return null
        val d = dir ?: return null
        val k = key(path) ?: return null
        return try {
            val f = File(d, k)
            if (!f.isFile) return null
            f.setLastModified(System.currentTimeMillis())
            BitmapFactory.decodeFile(f.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    fun put(path: String, bmp: Bitmap) {
        val d = dir ?: return
        val k = key(path) ?: return
        try {
            val f = File(d, k)
            f.outputStream().use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
        } catch (_: Exception) { }
    }

    /** 超过上限时按最久未用淘汰 */
    fun trim() {
        val d = dir ?: return
        try {
            val files = d.listFiles()?.sortedBy { it.lastModified() } ?: return
            var total = files.sumOf { it.length() }
            if (total <= MAX_BYTES) return
            for (f in files) {
                if (total <= MAX_BYTES) break
                total -= f.length()
                f.delete()
            }
        } catch (_: Exception) { }
    }

    fun clear() {
        try { dir?.listFiles()?.forEach { it.delete() } } catch (_: Exception) { }
    }
}
