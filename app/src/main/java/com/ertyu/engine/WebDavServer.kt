package com.ertyu.engine

import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WebDavServer(
    private val port: Int,
    private val rootDir: File,
    private val username: String,
    private val password: String
) {
    private var serverSocket: ServerSocket? = null
    private var running = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val dateFormat = SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    fun start() {
        running = true
        serverSocket = ServerSocket(port)
        scope.launch {
            while (running) {
                val socket = try {
                    serverSocket?.accept()
                } catch (e: Exception) {
                    if (running) null else break
                } ?: continue

                launch {
                    try {
                        handleClient(socket)
                    } catch (_: Exception) {
                    } finally {
                        try { socket.close() } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun stop() {
        running = false
        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
        scope.cancel()
    }

    // ==================== 请求处理 ====================

    private fun handleClient(socket: Socket) {
        socket.soTimeout = 30000
        val input = socket.getInputStream().buffered()
        val output = socket.getOutputStream().buffered()

        val requestLine = readLine(input) ?: return
        val parts = requestLine.split(" ")
        if (parts.size < 3) return
        val method = parts[0].uppercase()
        val rawUri = parts[1]

        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = readLine(input) ?: break
            if (line.isEmpty()) break
            val idx = line.indexOf(':')
            if (idx > 0) {
                headers[line.substring(0, idx).trim().lowercase()] =
                    line.substring(idx + 1).trim()
            }
        }

        if (!checkAuth(headers)) {
            writeResponse(
                output, 401, "text/plain",
                "Unauthorized".toByteArray(),
                mapOf("WWW-Authenticate" to "Basic realm=\"MTFM WebDAV\"")
            )
            return
        }

        // 读取 PUT body
        val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
        val chunked = headers["transfer-encoding"]
            ?.contains("chunked", ignoreCase = true) == true

        val response: HttpResp = when (method) {
            "OPTIONS" -> HttpResp(
                200, "text/plain", ByteArray(0), mapOf(
                    "DAV" to "1, 2",
                    "Allow" to "OPTIONS, GET, HEAD, PUT, DELETE, " +
                            "PROPFIND, MKCOL, MOVE, COPY",
                    "MS-Author-Via" to "DAV"
                )
            )
            "GET" -> handleGet(rawUri)
            "HEAD" -> handleHead(rawUri)
            "PUT" -> handlePut(rawUri, input, contentLength, chunked)
            "DELETE" -> handleDelete(rawUri)
            "PROPFIND" -> handlePropfind(rawUri, headers)
            "MKCOL" -> handleMkcol(rawUri)
            "MOVE" -> handleMove(rawUri, headers)
            "COPY" -> handleCopy(rawUri, headers)
            else -> HttpResp(
                405, "text/plain",
                "Method Not Allowed".toByteArray()
            )
        }

        writeResponse(output, response.status, response.contentType,
            response.body, response.extraHeaders)
    }

    private data class HttpResp(
        val status: Int,
        val contentType: String,
        val body: ByteArray,
        val extraHeaders: Map<String, String> = emptyMap()
    )

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\r'.code) {
                val next = input.read()
                if (next == '\n'.code) return sb.toString()
                if (next >= 0) sb.append(next.toChar())
                return sb.toString()
            }
            if (b == '\n'.code) return sb.toString()
            sb.append(b.toChar())
        }
    }

    private fun writeResponse(
        output: OutputStream,
        status: Int,
        contentType: String,
        body: ByteArray,
        extraHeaders: Map<String, String> = emptyMap()
    ) {
        val statusText = when (status) {
            200 -> "OK"
            201 -> "Created"
            204 -> "No Content"
            207 -> "Multi-Status"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            500 -> "Internal Server Error"
            else -> "OK"
        }
        val headers = buildString {
            append("HTTP/1.1 $status $statusText\r\n")
            append("Content-Type: $contentType\r\n")
            append("Content-Length: ${body.size}\r\n")
            append("Connection: close\r\n")
            append("Server: MTFM-WebDAV\r\n")
            extraHeaders.forEach { (k, v) ->
                append("$k: $v\r\n")
            }
            append("\r\n")
        }
        output.write(headers.toByteArray(Charsets.US_ASCII))
        if (body.isNotEmpty()) output.write(body)
        output.flush()
    }

    private fun checkAuth(headers: Map<String, String>): Boolean {
        val auth = headers["authorization"] ?: return false
        if (!auth.startsWith("Basic ", ignoreCase = true)) return false
        val decoded = try {
            String(Base64.decode(auth.substring(6).trim(), Base64.DEFAULT))
        } catch (e: Exception) {
            return false
        }
        return decoded == "$username:$password"
    }

    private fun decodeUri(uri: String): String {
        // 去掉 query
        val path = uri.substringBefore('?')
        return try {
            URLDecoder.decode(path, "UTF-8")
        } catch (e: Exception) {
            path
        }
    }

    private fun resolveFile(uri: String): File? {
        val decoded = decodeUri(uri)
        val rel = decoded.trimStart('/')
        val file = File(rootDir, rel)
        return try {
            val canonical = file.canonicalPath
            val rootCanonical = rootDir.canonicalPath
            if (!canonical.startsWith(rootCanonical)) null else file
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 各 HTTP 方法 ====================

    private fun handleGet(uri: String): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        if (!file.exists() || !file.isFile) {
            return HttpResp(404, "text/plain", "Not Found".toByteArray())
        }
        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            return HttpResp(500, "text/plain",
                "Read error: ${e.message}".toByteArray())
        }
        return HttpResp(
            status = 200,
            contentType = getMimeType(file.name),
            body = bytes,
            extraHeaders = mapOf(
                "Last-Modified" to
                        dateFormat.format(Date(file.lastModified()))
            )
        )
    }

    private fun handleHead(uri: String): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", ByteArray(0)
        )
        if (!file.exists()) {
            return HttpResp(404, "text/plain", ByteArray(0))
        }
        return HttpResp(
            status = 200,
            contentType = getMimeType(file.name),
            body = ByteArray(0),
            extraHeaders = mapOf(
                "Content-Length" to file.length().toString(),
                "Last-Modified" to
                        dateFormat.format(Date(file.lastModified()))
            )
        )
    }

    private fun handlePut(
        uri: String,
        input: InputStream,
        contentLength: Long,
        chunked: Boolean
    ): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        file.parentFile?.mkdirs()
        return try {
            file.outputStream().buffered().use { out ->
                if (chunked) {
                    readChunkedBody(input, out)
                } else if (contentLength > 0) {
                    val buf = ByteArray(64 * 1024)
                    var remaining = contentLength
                    while (remaining > 0) {
                        val toRead = minOf(buf.size.toLong(), remaining).toInt()
                        val n = input.read(buf, 0, toRead)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        remaining -= n
                    }
                } else {
                    // 无长度，读到 EOF
                    input.copyTo(out, 64 * 1024)
                }
            }
            HttpResp(201, "text/plain", "Created".toByteArray())
        } catch (e: Exception) {
            HttpResp(500, "text/plain",
                "Upload failed: ${e.message}".toByteArray())
        }
    }

    private fun readChunkedBody(input: InputStream, out: OutputStream) {
        while (true) {
            val sizeLine = readLine(input) ?: break
            val sizeStr = sizeLine.substringBefore(';').trim()
            val size = sizeStr.toIntOrNull(16) ?: break
            if (size == 0) {
                readLine(input) // trailing CRLF
                break
            }
            val buf = ByteArray(size)
            var read = 0
            while (read < size) {
                val n = input.read(buf, read, size - read)
                if (n < 0) break
                read += n
            }
            out.write(buf, 0, read)
            readLine(input) // CRLF after chunk
        }
    }

    private fun handleDelete(uri: String): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        if (!file.exists()) {
            return HttpResp(404, "text/plain", "Not Found".toByteArray())
        }
        val ok = if (file.isDirectory) file.deleteRecursively()
        else file.delete()
        return if (ok) HttpResp(204, "text/plain", ByteArray(0))
        else HttpResp(403, "text/plain", "Delete failed".toByteArray())
    }

    private fun handlePropfind(
        uri: String,
        headers: Map<String, String>
    ): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        if (!file.exists()) {
            return HttpResp(404, "text/plain", "Not Found".toByteArray())
        }
        val depth = headers["depth"]?.toIntOrNull() ?: 1
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.append("<D:multistatus xmlns:D=\"DAV:\">")
        appendPropfind(sb, file, uri)
        if (file.isDirectory && depth > 0) {
            file.listFiles()?.forEach { child ->
                val childUri = if (uri.endsWith("/")) "$uri${child.name}"
                else "$uri/${child.name}"
                appendPropfind(sb, child, childUri)
            }
        }
        sb.append("</D:multistatus>")
        return HttpResp(
            status = 207,
            contentType = "application/xml; charset=utf-8",
            body = sb.toString().toByteArray(Charsets.UTF_8)
        )
    }

    private fun appendPropfind(sb: StringBuilder, file: File, uri: String) {
        val href = encodeUri(uri)
        sb.append("<D:response>")
        sb.append("<D:href>").append(escapeXml(href)).append("</D:href>")
        sb.append("<D:propstat>")
        sb.append("<D:prop>")
        sb.append("<D:displayname>")
            .append(escapeXml(file.name))
            .append("</D:displayname>")
        if (file.isDirectory) {
            sb.append("<D:resourcetype><D:collection/></D:resourcetype>")
        } else {
            sb.append("<D:resourcetype/>")
            sb.append("<D:getcontentlength>")
                .append(file.length())
                .append("</D:getcontentlength>")
            sb.append("<D:getcontenttype>")
                .append(getMimeType(file.name))
                .append("</D:getcontenttype>")
        }
        sb.append("<D:getlastmodified>")
            .append(dateFormat.format(Date(file.lastModified())))
            .append("</D:getlastmodified>")
        sb.append("</D:prop>")
        sb.append("<D:status>HTTP/1.1 200 OK</D:status>")
        sb.append("</D:propstat>")
        sb.append("</D:response>")
    }

    private fun handleMkcol(uri: String): HttpResp {
        val file = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        if (file.exists()) {
            return HttpResp(405, "text/plain", "Exists".toByteArray())
        }
        return if (file.mkdirs()) {
            HttpResp(201, "text/plain", "Created".toByteArray())
        } else {
            HttpResp(500, "text/plain", "Create failed".toByteArray())
        }
    }

    private fun handleMove(
        uri: String,
        headers: Map<String, String>
    ): HttpResp {
        val src = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        val destHeader = headers["destination"] ?: return HttpResp(
            400, "text/plain", "No Destination".toByteArray()
        )
        val dst = resolveFile(destHeaderToPath(destHeader))
            ?: return HttpResp(403, "text/plain", "Forbidden".toByteArray())
        if (!src.exists()) {
            return HttpResp(404, "text/plain", "Not Found".toByteArray())
        }
        dst.parentFile?.mkdirs()
        if (dst.exists() && headers["overwrite"] != "F") {
            dst.deleteRecursively()
        }
        return if (src.renameTo(dst)) {
            HttpResp(201, "text/plain", "Moved".toByteArray())
        } else {
            HttpResp(500, "text/plain", "Move failed".toByteArray())
        }
    }

    private fun handleCopy(
        uri: String,
        headers: Map<String, String>
    ): HttpResp {
        val src = resolveFile(uri) ?: return HttpResp(
            403, "text/plain", "Forbidden".toByteArray()
        )
        val destHeader = headers["destination"] ?: return HttpResp(
            400, "text/plain", "No Destination".toByteArray()
        )
        val dst = resolveFile(destHeaderToPath(destHeader))
            ?: return HttpResp(403, "text/plain", "Forbidden".toByteArray())
        if (!src.exists()) {
            return HttpResp(404, "text/plain", "Not Found".toByteArray())
        }
        if (dst.exists() && headers["overwrite"] != "F") {
            dst.deleteRecursively()
        }
        return try {
            copyRecursive(src, dst)
            HttpResp(201, "text/plain", "Copied".toByteArray())
        } catch (e: Exception) {
            HttpResp(500, "text/plain",
                "Copy failed: ${e.message}".toByteArray())
        }
    }

    private fun copyRecursive(src: File, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { copyRecursive(it, File(dst, it.name)) }
        } else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
    }

    private fun destHeaderToPath(dest: String): String {
        return try {
            val url = java.net.URL(dest)
            url.path ?: "/"
        } catch (e: Exception) {
            dest
        }
    }

    private fun encodeUri(uri: String): String {
        return try {
            uri.split("/").joinToString("/") {
                URLEncoder.encode(it, "UTF-8").replace("+", "%20")
            }
        } catch (e: Exception) {
            uri
        }
    }

    private fun escapeXml(s: String): String {
        return s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getMimeType(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "log" -> "text/plain"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "zip" -> "application/zip"
            "apk" -> "application/vnd.android.package-archive"
            "pdf" -> "application/pdf"
            else -> "application/octet-stream"
        }
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (iface in Collections.list(interfaces)) {
                    if (!iface.isUp || iface.isLoopback) continue
                    for (addr in Collections.list(iface.inetAddresses)) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress ?: ""
                        }
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
            return ""
        }
    }
}