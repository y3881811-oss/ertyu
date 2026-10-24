package com.ertyu.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object ThumbnailLoader {
    private const val THUMB_PX = 96
    private const val CACHE_BYTES = 8 * 1024 * 1024

    private val cache = object : LruCache<String, Bitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val mutex = Mutex()

    suspend fun load(path: String, isVideo: Boolean = false): Bitmap? {
        if (path.isEmpty()) return null
        mutex.withLock { cache.get(path) }?.let { return it }

        val bmp = withContext(Dispatchers.IO.limitedParallelism(4)) {
            if (isVideo) decodeVideo(path) else decodeImage(path)
        } ?: return null

        mutex.withLock { cache.put(path, bmp) }
        return bmp
    }

    private fun decodeImage(path: String): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return null
            var sample = 1
            while (w / sample > THUMB_PX * 2 || h / sample > THUMB_PX * 2) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(path, opts)
        } catch (e: Exception) { null }
    }

    private fun decodeVideo(path: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(path)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            retriever.release()
            frame
        } catch (e: Exception) { null }
    }
}