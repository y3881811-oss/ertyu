package com.ertyu.engine

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder

/** 极简 WebDAV 客户端：列目录 / 下载 / 上传，够用且不引新依赖 */
object WebDavClient {

    data class Entry(
        val name: String,
        val path: String,
        val isDir: Boolean,
        val size: Long
    )

    private const val PROPFIND_BODY = """<?xml version="1.0" encoding="utf-8"?>
<d:propfind xmlns:d="DAV:"><d:prop>
<d:displayname/><d:getcontentlength/><d:resourcetype/>
</d:prop></d:propfind>"""

    fun normalizeBase(input: String): String {
        var s = input.trim()
        if (s.isEmpty()) return ""
        if (!s.startsWith("http://", true) && !s.startsWith("https://", true)) s = "http://$s"
        return s.trimEnd('/')
    }

    private fun authHeader(user: String, pass: String): String? {
        if (user.isBlank() && pass.isBlank()) return null
        val raw = "$user:$pass".toByteArray(Charsets.UTF_8)
        return "Basic " + Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    private fun open(url: String, method: String, user: String, pass: String): HttpURLConnection {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 12000
        c.readTimeout = 30000
        c.instanceFollowRedirects = true
        c.setRequestProperty("User-Agent", "EYU-FileManager")
        authHeader(user, pass)?.let { c.setRequestProperty("Authorization", it) }
        return c
    }

    private fun urlOf(base: String, path: String): String {
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }

    /** 把服务器返回的 href 转成站内路径 */
    private fun toPath(base: String, href: String, basePath: String): String {
        var h = href
        if (h.startsWith("http://", true) || h.startsWith("https://", true)) {
            val u = URL(h)
            h = u.path ?: "/"
        }
        h = try { URLDecoder.decode(h, "UTF-8") } catch (_: Exception) { h }
        val prefix = basePath.trimEnd('/')
        if (prefix.isNotEmpty() && h.startsWith(prefix)) h = h.removePrefix(prefix)
        if (!h.startsWith("/")) h = "/$h"
        return h
    }

    suspend fun list(base: String, path: String, user: String, pass: String): Result<List<Entry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = urlOf(base, path)
                val c = open(url, "PROPFIND", user, pass)
                try {
                    c.setRequestProperty("Depth", "1")
                    c.setRequestProperty("Content-Type", "application/xml; charset=utf-8")
                    c.doOutput = true
                    val body = PROPFIND_BODY.toByteArray(Charsets.UTF_8)
                    c.setFixedLengthStreamingMode(body.size)
                    c.outputStream.use { it.write(body) }

                    val code = c.responseCode
                    if (code != 207 && code != 200) {
                        val msg = when (code) {
                            401 -> "认证失败（401），检查账号密码"
                            403 -> "没有权限（403）"
                            404 -> "路径不存在（404）"
                            405 -> "服务器不支持 PROPFIND（$code）"
                            else -> "服务器返回 HTTP $code"
                        }
                        throw IllegalStateException(msg)
                    }
                    val xml = c.inputStream.bufferedReader().use { it.readText() }
                    parseEntries(base, path, xml)
                } finally {
                    c.disconnect()
                }
            }
        }

    /** 容错解析：不依赖 d:/D:/lp1: 前缀差异 */
    private fun parseEntries(base: String, currentPath: String, xml: String): List<Entry> {
        val result = ArrayList<Entry>()
        val responses = Regex("""<(?:\w+:)?response\b[\s\S]*?</(?:\w+:)?response>""", RegexOption.IGNORE_CASE)
            .findAll(xml)
        responses.forEach { m ->
            val chunk = m.value
            val href = Regex("""<(?:\w+:)?href>([\s\S]*?)</(?:\w+:)?href>""", RegexOption.IGNORE_CASE)
                .find(chunk)?.groupValues?.get(1)?.trim()?.let { unescapeXml(it) } ?: return@forEach
            val isDir = Regex("""<(?:\w+:)?collection\s*/>""", RegexOption.IGNORE_CASE).containsMatchIn(chunk)
            val size = Regex("""<(?:\w+:)?getcontentlength>(\d+)<""", RegexOption.IGNORE_CASE)
                .find(chunk)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            val path = toPath(base, href, currentPath)
            if (path.trimEnd('/') == currentPath.trimEnd('/')) return@forEach

            val rawName = path.trimEnd('/').substringAfterLast('/')
            val name = if (rawName.isBlank()) path else rawName
            result.add(Entry(name, if (isDir) path.trimEnd('/') + "/" else path, isDir, size))
        }
        return result.sortedWith(compareByDescending<Entry> { it.isDir }.thenBy { it.name.lowercase() })
    }

    suspend fun download(
        base: String,
        entry: Entry,
        target: File,
        user: String,
        pass: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(urlOf(base, entry.path), "GET", user, pass)
            try {
                val code = c.responseCode
                if (code != 200) throw IllegalStateException("下载失败：HTTP $code")
                val total = c.contentLengthLong.coerceAtLeast(0)
                target.parentFile?.mkdirs()
                var done = 0L
                c.inputStream.use { input ->
                    target.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
                AppLogger.i("WebDav", "下载完成 ${entry.path}（${ItemSizeEngine.format(target.length())}）")
                target
            } finally {
                c.disconnect()
            }
        }
    }

    suspend fun upload(
        base: String,
        remotePath: String,
        file: File,
        user: String,
        pass: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val c = open(urlOf(base, remotePath), "PUT", user, pass)
            try {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/octet-stream")
                val len = file.length()
                c.setFixedLengthStreamingMode(len)
                var done = 0L
                file.inputStream().use { input ->
                    c.outputStream.use { out ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (len > 0) onProgress((done.toFloat() / len.toFloat()).coerceIn(0f, 1f))
                        }
                    }
                }
                val code = c.responseCode
                if (code !in listOf(200, 201, 204)) throw IllegalStateException("上传失败：HTTP $code")
                AppLogger.i("WebDav", "上传完成 $remotePath")
            } finally {
                c.disconnect()
            }
        }
    }

    suspend fun mkcol(base: String, path: String, user: String, pass: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val c = open(urlOf(base, path), "MKCOL", user, pass)
                try {
                    val code = c.responseCode
                    if (code !in listOf(200, 201, 204, 405)) throw IllegalStateException("创建目录失败：HTTP $code")
                } finally {
                    c.disconnect()
                }
            }
        }

    private fun unescapeXml(s: String): String = s
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
}
