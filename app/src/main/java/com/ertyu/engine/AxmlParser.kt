package com.ertyu.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile

/**
 * 极简二进制 AndroidManifest.xml（AXML）解析器。
 * 只做两件事：还原成可读 XML 文本、收集 uses-permission。
 */
object AxmlParser {

    data class Result(
        val xml: String,
        val permissions: List<String>,
        val packageName: String
    )

    private const val TYPE_STRING_POOL = 0x0001
    private const val TYPE_XML = 0x0008
    private const val TYPE_XML_START_NAMESPACE = 0x0100
    private const val TYPE_XML_END_NAMESPACE = 0x0101
    private const val TYPE_XML_START_ELEMENT = 0x0102
    private const val TYPE_XML_END_ELEMENT = 0x0103
    private const val TYPE_XML_CDATA = 0x0104

    private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun parseApk(apkFile: java.io.File): Result? = try {
        ZipFile(apkFile).use { zip ->
            val entry = zip.getEntry("AndroidManifest.xml") ?: return null
            val bytes = zip.getInputStream(entry).use { it.readBytes() }
            parse(bytes)
        }
    } catch (_: Exception) {
        null
    }

    fun parse(bytes: ByteArray): Result? = try {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val headerType = buf.getShort(0).toInt() and 0xFFFF
        if (headerType != TYPE_XML) return null

        var pos = buf.getShort(2).toInt() and 0xFFFF // headerSize，通常 8
        var strings: List<String> = emptyList()
        val nsUris = HashMap<Int, String>()
        val nsPrefixes = HashMap<Int, String>()
        val out = StringBuilder()
        var depth = 0
        var packageName = ""
        val permissions = ArrayList<String>()

        val total = bytes.size
        while (pos + 8 <= total) {
            val type = buf.getShort(pos).toInt() and 0xFFFF
            val headerSize = buf.getShort(pos + 2).toInt() and 0xFFFF
            val size = buf.getInt(pos + 4)
            if (size <= 0 || pos + size > total) break

            when (type) {
                TYPE_STRING_POOL -> {
                    strings = readStringPool(buf, pos)
                }
                TYPE_XML_START_NAMESPACE -> {
                    val prefix = buf.getInt(pos + 16)
                    val uri = buf.getInt(pos + 20)
                    if (uri >= 0) nsUris[uri] = safeGet(strings, uri)
                    if (prefix >= 0) nsPrefixes[prefix] = safeGet(strings, prefix)
                }
                TYPE_XML_START_ELEMENT -> {
                    val nodeStart = pos
                    val nameIdx = buf.getInt(nodeStart + 20)
                    val attrStart = buf.getShort(nodeStart + 24).toInt() and 0xFFFF
                    val attrSize = buf.getShort(nodeStart + 26).toInt() and 0xFFFF
                    val attrCount = buf.getShort(nodeStart + 28).toInt() and 0xFFFF
                    val name = safeGet(strings, nameIdx)
                    val attrs = readAttributes(buf, nodeStart + 16 + attrStart, attrCount, attrSize, strings, nsUris)

                    indent(out, depth)
                    out.append('<').append(name)
                    attrs.forEach { (attrName, value) ->
                        out.append(' ').append(attrName).append("=\"")
                        out.append(escape(value))
                        out.append('"')
                    }
                    out.append(">\n")

                    if (name == "manifest") {
                        attrs.firstOrNull { it.first == "package" }?.let { packageName = it.second }
                    }
                    if (name == "uses-permission" || name == "uses-permission-sdk-23") {
                        attrs.firstOrNull { it.first.endsWith("name") }?.let { permissions.add(it.second) }
                    }
                    depth++
                }
                TYPE_XML_END_ELEMENT -> {
                    val nameIdx = buf.getInt(pos + 20)
                    val name = safeGet(strings, nameIdx)
                    depth = (depth - 1).coerceAtLeast(0)
                    indent(out, depth)
                    out.append("</").append(name).append(">\n")
                }
                TYPE_XML_CDATA -> {
                    val dataIdx = buf.getInt(pos + 16)
                    indent(out, depth)
                    out.append("<![CDATA[").append(escape(safeGet(strings, dataIdx))).append("]]>\n")
                }
                TYPE_XML_END_NAMESPACE -> Unit
            }
            pos += size
        }
        Result(out.toString(), permissions.distinct(), packageName)
    } catch (_: Exception) {
        null
    }

    private fun readStringPool(buf: ByteBuffer, start: Int): List<String> {
        val count = buf.getInt(start + 8)
        val flags = buf.getInt(start + 16)
        val stringsStart = buf.getInt(start + 20)
        val utf8 = (flags and (1 shl 8)) != 0
        val result = ArrayList<String>(count)
        val offsetsBase = start + 28
        for (i in 0 until count) {
            val offset = buf.getInt(offsetsBase + i * 4)
            val at = start + stringsStart + offset
            result.add(if (utf8) readUtf8(buf, at) else readUtf16(buf, at))
        }
        return result
    }

    private fun readUtf8(buf: ByteBuffer, at: Int): String {
        var p = at
        var b = buf.get(p).toInt() and 0xFF
        if (b and 0x80 != 0) { p += 1; b = ((b and 0x7F) shl 8) or (buf.get(p).toInt() and 0xFF) }
        p += 1
        var len = buf.get(p).toInt() and 0xFF
        if (len and 0x80 != 0) {
            len = ((len and 0x7F) shl 8) or (buf.get(p + 1).toInt() and 0xFF)
            p += 2
        } else {
            p += 1
        }
        val bytes = ByteArray(len)
        for (i in 0 until len) bytes[i] = buf.get(p + i)
        return String(bytes, Charsets.UTF_8)
    }

    private fun readUtf16(buf: ByteBuffer, at: Int): String {
        var p = at
        var len = buf.getShort(p).toInt() and 0xFFFF
        if (len and 0x8000 != 0) {
            len = ((len and 0x7FFF) shl 16) or (buf.getShort(p + 2).toInt() and 0xFFFF)
            p += 4
        } else {
            p += 2
        }
        val chars = CharArray(len)
        for (i in 0 until len) chars[i] = buf.getChar(p + i * 2)
        return String(chars)
    }

    private fun readAttributes(
        buf: ByteBuffer,
        at: Int,
        count: Int,
        attrSize: Int,
        strings: List<String>,
        nsUris: Map<Int, String>
    ): List<Pair<String, String>> {
        if (count <= 0) return emptyList()
        val step = if (attrSize > 0) attrSize else 20
        val out = ArrayList<Pair<String, String>>(count)
        for (i in 0 until count) {
            val p = at + i * step
            val nsIdx = buf.getInt(p)
            val nameIdx = buf.getInt(p + 4)
            val rawIdx = buf.getInt(p + 8)
            val dataType = buf.get(p + 15).toInt() and 0xFF
            val data = buf.getInt(p + 16)

            val ns = nsUris[nsIdx]
            val prefix = if (ns == ANDROID_NS) "android" else if (ns.isNullOrBlank()) "" else "ns"
            var name = safeGet(strings, nameIdx)
            if (prefix.isNotEmpty()) name = "$prefix:$name"

            val value = when {
                rawIdx != -1 -> safeGet(strings, rawIdx)
                dataType == 0x03 -> safeGet(strings, data)
                dataType == 0x12 -> (data != 0).toString()
                dataType == 0x10 -> data.toString()
                dataType == 0x11 -> "0x%08x".format(data)
                dataType == 0x01 -> "@ref/0x%08x".format(data)
                dataType == 0x04 -> java.lang.Float.intBitsToFloat(data).toString()
                else -> "0x%08x".format(data)
            }
            out.add(name to value)
        }
        return out
    }

    private fun safeGet(list: List<String>, idx: Int): String =
        if (idx in list.indices) list[idx] else ""

    private fun indent(sb: StringBuilder, depth: Int) {
        repeat(depth) { sb.append("    ") }
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
