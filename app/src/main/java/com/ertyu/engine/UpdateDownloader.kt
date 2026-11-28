package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object UpdateDownloader {

    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 30000
    private const val SEGMENTS = 4
    private const val MIN_MULTI_BYTES = 2L * 1024 * 1024
    private const val MAX_RETRY = 3
    private const val UA = "EYU-FileManager"

    suspend fun fetchText(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
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

    private fun open(url: String, range: String?): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", UA)
            if (range != null) setRequestProperty("Range", range)
        }

    private data class Probe(val total: Long, val rangeOk: Boolean)

    private fun probe(url: String): Probe {
        val conn = open(url, "bytes=0-0")
        return try {
            val code = conn.responseCode
            if (code == 206) {
                val total = conn.getHeaderField("Content-Range")
                    ?.substringAfterLast('/')?.trim()?.toLongOrNull() ?: -1L
                Probe(total, total > 0)
            } else {
                Probe(conn.contentLengthLong, false)
            }
        } catch (_: Exception) {
            Probe(-1, false)
        } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    /**
     * 下载 App 更新：服务器支持 Range 且文件够大时用多线程分片，
     * 否则退回单线程断点续传。分片各自留 .partN，可跨次续传。
     */
    suspend fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            dest.parentFile?.mkdirs()
            val legacyPart = File(dest.parentFile, dest.name + ".part")
            val p = probe(url)

            val useMulti = p.rangeOk && p.total >= MIN_MULTI_BYTES && !legacyPart.isFile
            if (useMulti) {
                AppLogger.i("Update", "多线程下载：${ItemSizeEngine.format(p.total)}，$SEGMENTS 线程")
                try {
                    multiDownload(url, dest, p.total, onProgress)
                } catch (e: Exception) {
                    // 分片不可用（服务器忽略 Range 等）→ 清掉分片，退回单线程
                    AppLogger.e("Update", "多线程失败，改单线程", e)
                    (0 until SEGMENTS).forEach {
                        File(dest.parentFile, "${dest.name}.part.$it").delete()
                    }
                    File(dest.parentFile, dest.name + ".part").delete()
                    singleDownload(url, dest, onProgress)
                }
            } else {
                if (p.rangeOk && legacyPart.isFile) {
                    AppLogger.i("Update", "发现旧的 .part，继续单线程续传")
                }
                singleDownload(url, dest, onProgress)
            }
        }
    }

    // ---------------- 多线程 ----------------

    private suspend fun multiDownload(
        url: String,
        dest: File,
        total: Long,
        onProgress: (Long, Long) -> Unit
    ) {
        val base = File(dest.parentFile, dest.name + ".part")
        val segLen = (total + SEGMENTS - 1) / SEGMENTS
        val sizes = LongArray(SEGMENTS)
        var lastPercent = -1

        fun report() {
            val done = sizes.sum()
            val percent = if (total > 0) ((done * 100) / total).toInt() else -1
            if (percent != lastPercent) {
                lastPercent = percent
                onProgress(done, total)
            }
        }

        coroutineScope {
            (0 until SEGMENTS).map { i ->
                async(Dispatchers.IO) {
                    val start = i * segLen
                    val end = minOf(total - 1, start + segLen - 1)
                    if (start > end) return@async
                    val segFile = File(dest.parentFile, "${base.name}.$i")
                    // 断点：已有长度作为起点
                    if (segFile.isFile && segFile.length() > end - start + 1) {
                        segFile.delete()
                    }
                    sizes[i] = if (segFile.isFile) segFile.length() else 0L
                    report()

                    var attempt = 0
                    while (true) {
                        val have = if (segFile.isFile) segFile.length() else 0L
                        val want = end - start + 1
                        if (have >= want) return@async
                        val from = start + have
                        try {
                            val conn = open(url, "bytes=$from-$end")
                            val code = conn.responseCode
                            if (code != 206) {
                                conn.disconnect()
                                throw IllegalStateException("分片请求返回 $code")
                            }
                            conn.inputStream.use { input ->
                                FileOutputStream(segFile, true).use { out ->
                                    val buf = ByteArray(64 * 1024)
                                    while (true) {
                                        while (UpdatePause.paused) delay(150)
                                        val n = input.read(buf)
                                        if (n < 0) break
                                        out.write(buf, 0, n)
                                        sizes[i] = segFile.length()
                                        report()
                                    }
                                }
                            }
                            conn.disconnect()
                        } catch (e: Exception) {
                            attempt++
                            AppLogger.e("Update", "分片 $i 第 $attempt 次失败", e)
                            if (attempt >= MAX_RETRY) throw e
                            delay(800L * attempt)
                        }
                    }
                }
            }.awaitAll()
        }

        // 合并分片
        val merged = base
        if (merged.exists()) merged.delete()
        FileOutputStream(merged).use { out ->
            val buf = ByteArray(256 * 1024)
            for (i in 0 until SEGMENTS) {
                val segFile = File(dest.parentFile, "${base.name}.$i")
                if (!segFile.isFile) continue
                segFile.inputStream().use { input ->
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                    }
                }
            }
        }
        val expected = total
        if (merged.length() != expected) {
            throw IllegalStateException(
                "下载不完整（${ItemSizeEngine.format(merged.length())}/${ItemSizeEngine.format(expected)}），可重试继续"
            )
        }
        (0 until SEGMENTS).forEach { File(dest.parentFile, "${base.name}.$it").delete() }
        finish(merged, dest)
        onProgress(dest.length(), expected)
    }

    // ---------------- 单线程 ----------------

    private fun singleDownload(url: String, dest: File, onProgress: (Long, Long) -> Unit) {
        val part = File(dest.parentFile, dest.name + ".part")
        var existing = if (part.isFile) part.length() else 0L
        if (existing > 0) {
            AppLogger.i("Update", "发现未完成的下载（${ItemSizeEngine.format(existing)}），尝试续传")
        }

        val conn = open(url, if (existing > 0) "bytes=$existing-" else null)
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
                        while (UpdatePause.paused) {
                            Thread.sleep(150)
                        }
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
                throw IllegalStateException(
                    "下载不完整（${ItemSizeEngine.format(part.length())}/${ItemSizeEngine.format(total)}），可重试继续"
                )
            }
            finish(part, dest)
            onProgress(dest.length(), if (total > 0) total else dest.length())
        } finally {
            try { conn.disconnect() } catch (_: Exception) {}
        }
    }

    private fun finish(part: File, dest: File) {
        if (dest.exists()) dest.delete()
        if (!part.renameTo(dest)) {
            part.copyTo(dest, overwrite = true)
            part.delete()
        }
    }
}

/** 下载暂停开关（由界面上「暂停/继续」控制） */
object UpdatePause {
    @Volatile
    var paused: Boolean = false
}
