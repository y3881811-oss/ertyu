package com.ertyu.engine

import android.util.Base64
import fi.iki.elonen.NanoHTTPD
import java.io.File
import java.io.FileInputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class WebDavServer(
    port: Int,
    private val rootDir: File,
    private val username: String,
    private val password: String
) : NanoHTTPD(port) {

    private val dateFormat = SimpleDateFormat(
        "EEE, dd MMM yyyy HH:mm:ss 'GMT'",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("GMT")
    }

    override fun serve(session: IHTTPSession): Response {
        if (!checkAuth(session)) {
            return newFixedLengthResponse(
                Response.Status.UNAUTHORIZED,
                "text/plain",
                "Unauthorized"
            ).apply {
                addHeader("WWW-Authenticate", "Basic realm=\"MTFM WebDAV\"")
            }
        }

        val uri = session.uri ?: "/"
        return try {
            when (session.method) {
                Method.OPTIONS -> handleOptions()
                Method.GET -> handleGet(uri)
                Method.HEAD -> handleHead(uri)
                Method.PUT -> handlePut(uri, session)
                Method.DELETE -> handleDelete(uri)
                Method.PROPFIND -> handlePropfind(uri, session)
                Method.MKCOL -> handleMkcol(uri)
                Method.MOVE -> handleMove(uri, session)
                Method.COPY -> handleCopy(uri, session)
                else -> newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    "text/plain",
                    "Method not allowed"
                )
            }
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Error: ${e.message}"
            )
        }
    }

    private fun checkAuth(session: IHTTPSession): Boolean {
        val auth = session.headers["authorization"] ?: return false
        if (!auth.startsWith("Basic ")) return false
        val decoded = try {
            String(Base64.decode(auth.substring(6).trim(), Base64.DEFAULT))
        } catch (e: Exception) {
            return false
        }
        return decoded == "$username:$password"
    }

    private fun handleOptions(): Response {
        return newFixedLengthResponse(Response.Status.OK, "text/plain", "")
            .apply {
                addHeader("DAV", "1, 2")
                addHeader(
                    "Allow",
                    "OPTIONS, GET, HEAD, PUT, DELETE, PROPFIND, MKCOL, MOVE, COPY"
                )
                addHeader("MS-Author-Via", "DAV")
            }
    }

    private fun resolveFile(uri: String): File? {
        val decoded = try {
            URLDecoder.decode(uri, "UTF-8")
        } catch (e: Exception) {
            uri
        }
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

    private fun handleGet(uri: String): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (!file.exists() || !file.isFile) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }
        val mime = getMimeType(file.name)
        val input = FileInputStream(file)
        return newChunkedResponse(Response.Status.OK, mime, input)
            .apply {
                addHeader("Content-Length", file.length().toString())
                addHeader(
                    "Last-Modified",
                    dateFormat.format(Date(file.lastModified()))
                )
            }
    }

    private fun handleHead(uri: String): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", ""
            )
        if (!file.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", ""
            )
        }
        return newFixedLengthResponse(
            Response.Status.OK, getMimeType(file.name), ""
        ).apply {
            addHeader("Content-Length", file.length().toString())
            addHeader(
                "Last-Modified",
                dateFormat.format(Date(file.lastModified()))
            )
        }
    }

    private fun handlePut(uri: String, session: IHTTPSession): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        file.parentFile?.mkdirs()

        return try {
            session.inputStream.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output, 64 * 1024)
                }
            }
            newFixedLengthResponse(
                Response.Status.CREATED, "text/plain", "Created"
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Upload failed: ${e.message}"
            )
        }
    }

    private fun handleDelete(uri: String): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (!file.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }
        val ok = if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
        return if (ok) {
            newFixedLengthResponse(
                Response.Status.NO_CONTENT, "text/plain", ""
            )
        } else {
            newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Delete failed"
            )
        }
    }

    private fun handlePropfind(uri: String, session: IHTTPSession): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (!file.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }

        val depth = session.headers["depth"]?.toIntOrNull() ?: 1
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        sb.append("<D:multistatus xmlns:D=\"DAV:\">")
        appendPropfindResponse(sb, file, uri)
        if (file.isDirectory && depth > 0) {
            file.listFiles()?.forEach { child ->
                val childUri = if (uri.endsWith("/")) {
                    "$uri${child.name}"
                } else {
                    "$uri/${child.name}"
                }
                appendPropfindResponse(sb, child, childUri)
            }
        }
        sb.append("</D:multistatus>")

        return newFixedLengthResponse(
            MultiStatus,
            "application/xml; charset=utf-8",
            sb.toString()
        )
    }

    private fun appendPropfindResponse(
        sb: StringBuilder,
        file: File,
        uri: String
    ) {
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

    private fun handleMkcol(uri: String): Response {
        val file = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (file.exists()) {
            return newFixedLengthResponse(
                Response.Status.METHOD_NOT_ALLOWED,
                "text/plain",
                "Already exists"
            )
        }
        return if (file.mkdirs()) {
            newFixedLengthResponse(
                Response.Status.CREATED, "text/plain", "Created"
            )
        } else {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Create failed"
            )
        }
    }

    private fun handleMove(uri: String, session: IHTTPSession): Response {
        val src = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        val destHeader = session.headers["destination"]
            ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "No destination"
            )
        val destUri = destHeaderToUri(destHeader)
        val dst = resolveFile(destUri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (!src.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }
        dst.parentFile?.mkdirs()
        val overwrite = session.headers["overwrite"] != "F"
        if (dst.exists() && overwrite) dst.deleteRecursively()

        val ok = src.renameTo(dst)
        return if (ok) {
            newFixedLengthResponse(
                Response.Status.CREATED, "text/plain", "Moved"
            )
        } else {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Move failed"
            )
        }
    }

    private fun handleCopy(uri: String, session: IHTTPSession): Response {
        val src = resolveFile(uri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        val destHeader = session.headers["destination"]
            ?: return newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "text/plain",
                "No destination"
            )
        val destUri = destHeaderToUri(destHeader)
        val dst = resolveFile(destUri)
            ?: return newFixedLengthResponse(
                Response.Status.FORBIDDEN, "text/plain", "Forbidden"
            )
        if (!src.exists()) {
            return newFixedLengthResponse(
                Response.Status.NOT_FOUND, "text/plain", "Not found"
            )
        }
        val overwrite = session.headers["overwrite"] != "F"
        if (dst.exists() && overwrite) dst.deleteRecursively()

        val ok = try {
            copyRecursive(src, dst)
            true
        } catch (e: Exception) {
            false
        }
        return if (ok) {
            newFixedLengthResponse(
                Response.Status.CREATED, "text/plain", "Copied"
            )
        } else {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Copy failed"
            )
        }
    }

    private fun copyRecursive(src: File, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { child ->
                copyRecursive(child, File(dst, child.name))
            }
        } else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
    }

    private fun destHeaderToUri(dest: String): String {
        return try {
            val url = java.net.URL(dest)
            url.path
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
        val MultiStatus: Response.IStatus = object : Response.IStatus {
            override fun getRequestStatus(): Int = 207
            override fun getDescription(): String = "Multi-Status"
        }

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