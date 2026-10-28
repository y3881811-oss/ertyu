package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dongliu.apk.parser.ApkFile
import java.io.File
import java.util.zip.ZipFile

object ApkParserEngine {

    suspend fun getManifest(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile(apkFile).use { apk -> apk.manifestXml }
        }
    }

    suspend fun getResourcesArsc(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile(apkFile).use { apk ->
                val sb = StringBuilder()
                sb.appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                sb.appendLine("<!-- 资源表预览 (resources.arsc) 基础信息 -->")
                sb.appendLine("<resources>")
                val meta = apk.apkMeta
                sb.appendLine("  <string name=\"package\">${meta.packageName}</string>")
                sb.appendLine("  <string name=\"versionName\">${meta.versionName}</string>")
                sb.appendLine("  <string name=\"versionCode\">${meta.versionCode}</string>")
                sb.appendLine("  <string name=\"minSdkVersion\">${meta.minSdkVersion}</string>")
                sb.appendLine("  <string name=\"targetSdkVersion\">${meta.targetSdkVersion}</string>")
                if (meta.usesPermissions.isNotEmpty()) {
                    sb.appendLine("\n  <!-- 权限列表 -->")
                    meta.usesPermissions.forEach { perm ->
                        sb.appendLine("  <uses-permission name=\"${perm.name}\" />")
                    }
                }
                sb.appendLine("</resources>")
                sb.toString()
            }
        }
    }

    suspend fun getInternalFileContent(apkFile: File, entryPath: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(entryPath) ?: throw IllegalStateException("找不到文件")
                zip.getInputStream(entry).use { input ->
                    val bytes = input.readBytes()
                    if (isText(bytes)) {
                        String(bytes, Charsets.UTF_8)
                    } else {
                        "<!-- 该文件是二进制格式（未解码的 XML 或其他资源），不支持直接文本预览。\n请使用右上角菜单将其提取到本地后，用 HEX 查看器打开。 -->"
                    }
                }
            }
        }
    }

    private fun isText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        var textCount = 0
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0x20..0x7E || i == 0x0A || i == 0x0D || i == 0x09) textCount++
            else if (i > 0x7F) textCount++ 
        }
        return textCount > bytes.size * 0.8
    }
}