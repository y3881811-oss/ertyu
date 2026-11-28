package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateDownloader {

    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 30000

    suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "EYU-FileManager")
            }
            if (conn.responseCode != 200) {
                conn.disconnect()
                return@withContext null
            }
            conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 下载并支持断点续传：先写 xxx.apk.part，网络中断后下次用 Range 头接着下。
     * 服务器不支持 Range（返回 200）时会自动从头重下。
     */
    suspend fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dest.parentFile?.mkdirs()
            val part = File(dest.parentFile, dest.name + ".part")
            var existing = if (part.isFile) part.length() else 0L
            if (existing > 0) {
                AppLogger.i("Update", "发现未完成的下载（${ItemSizeEngine.format(existing)}），尝试续传")
            }

            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "EYU-FileManager")
                if (existing > 0) setRequestProperty("Range", "bytes=$existing-")
            }
            try {
                val code = conn.responseCode
                if (code != 200 && code != 206) throw IllegalStateException("下载失败：HTTP $code")

                var total = conn.contentLengthLong.coerceAtLeast(0L)
                val resumed = code == 206 && existing > 0
                if (!resumed) existing = 0L
                if (total > 0) total += existing
                if (!resumed && total in 1..1024) {
                    throw IllegalStateException("下载到的内容异常（$total 字节）")
                }

                var done = existing
                var lastPercent = -1
                conn.inputStream.use { input ->
                    FileOutputStream(part, resumed).use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            while (UpdatePause.paused) delay(150)
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            val percent = if (total > 0) ((done * 100) / total).toInt() else -1
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(done, total)
                            }
                        }
                    }
                }

                if (part.length() <= 0) throw IllegalStateException("下载内容为空")
                if (total > 0 && part.length() < total) {
                    throw IllegalStateException("下载不完整（${ItemSizeEngine.format(part.length())}/${ItemSizeEngine.format(total)}），可重试继续")
                }
                if (dest.exists()) dest.delete()
                if (!part.renameTo(dest)) {
                    part.copyTo(dest, overwrite = true)
                    part.delete()
                }
                onProgress(dest.length(), if (total > 0) total else dest.length())
            } finally {
                conn.disconnect()
            }
        }
    }
}

/** 下载暂停开关（由界面上「暂停/继续」控制） */
object UpdatePause {
    @Volatile
    var paused: Boolean = false
}
