package com.ertyu.engine

import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

object ApkParserEngine {

    suspend fun getManifest(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkModule.loadApkFile(apkFile).use { apkModule ->
                val manifestBlock: AndroidManifestBlock = apkModule.androidManifest
                // 使用父类 ResXmlDocument 的 serializeToXml() 方法，它返回 String
                manifestBlock.serializeToXml()
            }
        }
    }

    suspend fun getResourcesArsc(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkModule.loadApkFile(apkFile).use { apkModule ->
                val manifestBlock: AndroidManifestBlock = apkModule.androidManifest

                // 安全获取信息，处理可能的 null 值
                val packageName = manifestBlock.packageName ?: ""
                val versionName = manifestBlock.versionName ?: ""
                val versionCode = manifestBlock.versionCode?.toString() ?: "0"
                val minSdk = manifestBlock.minSdkVersion?.toString() ?: "0"
                val targetSdk = manifestBlock.targetSdkVersion?.toString() ?: "0"

                // getUsesPermissions() 直接返回 List<String>
                val permissions = manifestBlock.usesPermissions ?: emptyList()

                val sb = StringBuilder()
                sb.appendLine("<h1>APK 信息</h1>")
                sb.appendLine("<h2>基本信息</h2>")
                sb.appendLine("<p>包名: $packageName</p>")
                sb.appendLine("<p>版本名: $versionName</p>")
                sb.appendLine("<p>版本号: $versionCode</p>")
                sb.appendLine("<p>最低 SDK: $minSdk</p>")
                sb.appendLine("<p>目标 SDK: $targetSdk</p>")

                if (permissions.isNotEmpty()) {
                    sb.appendLine("\n<h2>权限列表</h2>")
                    permissions.forEach { perm ->
                        sb.appendLine("<p>$perm</p>")
                    }
                }
                sb.toString()
            }
        }
    }

    suspend fun getInternalFileContent(apkFile: File, entryPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(apkFile).use { zip ->
                    val entry = zip.getEntry(entryPath)
                        ?: throw IllegalStateException("找不到文件: $entryPath")
                    zip.getInputStream(entry).use { input ->
                        val bytes = input.readBytes()
                        if (isText(bytes)) String(bytes, Charsets.UTF_8) else ""
                    }
                }
            }
        }

    private fun isText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        var textCount = 0
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0x20..0x7E || i == 0x0A || i == 0x0D || i == 0x09 || i > 0x7F) textCount++
        }
        return textCount > bytes.size * 0.8
    }
}