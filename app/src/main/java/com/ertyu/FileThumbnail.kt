package com.ertyu.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 图片缩略图加载器。
 * - 内存 LRU 缓存（8MB）
 * - 4 路并发解码
 * - BitmapFactory.inSampleSize 缩小，节省内存
 */
object ThumbnailLoader {

    private const val THUMB_PX = 96
    private const val CACHE_BYTES = 8 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val mutex = Mutex()

    suspend fun load(path: String): Bitmap? {
        if (path.isEmpty()) return null

        mutex.withLock { cache.get(path) }?.let { return it }

        val bmp = withContext(Dispatchers.IO.limitedParallelism(4)) {
            decodeThumbnail(path)
        } ?: return null

        mutex.withLock { cache.put(path, bmp) }
        return bmp
    }

    private fun decodeThumbnail(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(path, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null

            var sample = 1
            while (w / sample > THUMB_PX * 2 || h / sample > THUMB_PX * 2) {
                sample *= 2
            }

            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) {
            null
        }
    }

    fun clear() {
        cache.evictAll()
    }
}