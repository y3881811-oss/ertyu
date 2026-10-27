package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.ApkFile
import java.io.File

object ApkParserEngine {

    /**
     * 获取解码后的 AndroidManifest.xml
     */
    suspend fun getManifest(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile(apkFile).use { apk ->
                apk.manifestXml
            }
        }
    }

    /**
     * 获取 resources.arsc 的字符串内容
     */
    suspend fun getResourcesArsc(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile(apkFile).use { apk ->
                val sb = StringBuilder()
                sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                sb.appendLine("<!-- 资源表预览 (resources.arsc)，由于 ARSC 极其庞大，此处仅提取字符串资源 -->")
                sb.appendLine("<resources>")
                
                val stringResources = apk.translator.translate()
                stringResources?.forEach { (key, value) ->
                    sb.appendLine("  <string name=\"$key\">${escapeXml(value)}</string>")
                }
                
                sb.appendLine("</resources>")
                sb.toString()
            }
        }
    }

    /**
     * 获取 APK 内部指定文件的内容
     */
    suspend fun getInternalFileContent(apkFile: File, entryPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile(apkFile).use { apk ->
                if (entryPath.endsWith(".xml")) {
                    // 借助库的解析能力处理 res/ 下的二进制 XML
                    try {
                        apk.translator.translate(entryPath)?.let { return@withContext Result.success(it) }
                    } catch (e: Exception) {
                        // 如果翻译失败，回退到获取原始文本
                    }
                }
                
                val bytes = apk.getFileContent(entryPath)
                    ?: throw IllegalStateException("无法读取文件")
                
                if (isText(bytes)) {
                    String(bytes, Charsets.UTF_8)
                } else {
                    "<!-- 二进制文件，不支持直接预览，请提取后使用 HEX 查看器 -->"
                }
            }
        }
    }

    private fun isText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        var textCount = 0
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0x20..0x7E || i == 0x0A || i == 0x0D || i == 0x09) {
                textCount++
            } else if (i > 0x7F) {
                textCount++ // 简单包容中文等 UTF-8 字符
            }
        }
        return textCount > bytes.size * 0.8
    }

    private fun escapeXml(input: String): String {
        return input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}