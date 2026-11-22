package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

    suspend fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dest.parentFile?.mkdirs()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "EYU-FileManager")
            }
            try {
                val code = conn.responseCode
                if (code != 200) throw IllegalStateException("下载失败：HTTP $code")
                val total = conn.contentLengthLong.coerceAtLeast(0L)
                if (total in 1..1024) throw IllegalStateException("下载到的内容异常（$total 字节）")

                val tmp = File(dest.parentFile, dest.name + ".part")
                var done = 0L
                var lastPercent = -1
                conn.inputStream.use { input ->
                    tmp.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
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
                if (tmp.length() <= 0) throw IllegalStateException("下载内容为空")
                if (dest.exists()) dest.delete()
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                onProgress(dest.length(), if (total > 0) total else dest.length())
            } finally {
                conn.disconnect()
            }
        }
    }
}
