package com.ertyu.engine

import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

/**
 * 文本编码自动识别：BOM → 严格 UTF-8 → GBK 兜底。
 * 写入时沿用打开时的编码，避免把 GBK 文件悄悄改成 UTF-8。
 */
object TextCodec {
    private val charsetByPath = HashMap<String, Charset>()

    fun charsetOf(path: String): String =
        (charsetByPath[path] ?: StandardCharsets.UTF_8).name()

    fun read(file: File): String? {
        val bytes = try {
            file.readBytes()
        } catch (_: Exception) {
            return null
        }
        if (bytes.isEmpty()) {
            charsetByPath[file.absolutePath] = StandardCharsets.UTF_8
            return ""
        }
        // UTF-8 BOM
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() &&
            bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) {
            charsetByPath[file.absolutePath] = StandardCharsets.UTF_8
            return String(bytes, 3, bytes.size - 3, StandardCharsets.UTF_8)
        }
        // UTF-16 BOM
        if (bytes.size >= 2) {
            val le = bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()
            val be = bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()
            if (le || be) {
                val cs = if (le) StandardCharsets.UTF_16LE else StandardCharsets.UTF_16BE
                charsetByPath[file.absolutePath] = cs
                return String(bytes, 2, bytes.size - 2, cs)
            }
        }
        // 严格 UTF-8
        return try {
            val decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            val text = decoder.decode(ByteBuffer.wrap(bytes)).toString()
            charsetByPath[file.absolutePath] = StandardCharsets.UTF_8
            text
        } catch (_: Exception) {
            // GBK 兜底（中文 Windows 文本常见）
            val gbk = try {
                Charset.forName("GBK")
            } catch (_: Exception) {
                null
            }
            if (gbk != null) {
                charsetByPath[file.absolutePath] = gbk
                String(bytes, gbk)
            } else {
                charsetByPath[file.absolutePath] = StandardCharsets.UTF_8
                String(bytes, StandardCharsets.UTF_8)
            }
        }
    }

    fun write(file: File, content: String): Boolean = try {
        val cs = charsetByPath[file.absolutePath] ?: StandardCharsets.UTF_8
        file.parentFile?.mkdirs()
        file.writeBytes(content.toByteArray(cs))
        true
    } catch (_: Exception) {
        false
    }
}
