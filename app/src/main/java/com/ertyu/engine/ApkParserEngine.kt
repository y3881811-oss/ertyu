package com.ertyu.engine

import com.reandroid.apk.ApkFile
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

object ApkParserEngine {

    /**
     * 获取反编译后的 AndroidManifest.xml 字符串
     * 使用 APKEditor / ARSCLib 的 AndroidManifestBlock 解码二进制 AXML
     */
    suspend fun getManifest(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile.load(apkFile).use { apk ->
                val manifestBlock: AndroidManifestBlock = apk.androidManifest
                // decodeXml() 将二进制 AXML 转为可读的 XML 文本
                manifestBlock.decodeXml().toString()
            }
        }
    }

    /**
     * 获取 APK 的元信息（包名、版本、SDK、权限等），并以 HTML 形式返回
     * 直接从 AndroidManifestBlock 读取，不需要解析 resources.arsc
     */
    suspend fun getResourcesArsc(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ApkFile.load(apkFile).use { apk ->
                val manifestBlock: AndroidManifestBlock = apk.androidManifest

                val packageName = manifestBlock.packageName ?: ""
                val versionName = manifestBlock.versionName ?: ""
                val versionCode = manifestBlock.versionCode.toString()
                val minSdk = manifestBlock.minSdkVersion.toString()
                val targetSdk = manifestBlock.targetSdkVersion.toString()

                // 获取权限列表；不同版本 ARSCLib 的 API 名称可能略有差异
                val permissions = manifestBlock
                    .getUsesPermissions()
                    ?.mapNotNull { it.name }
                    ?: emptyList()

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

    /**
     * 读取 APK 内部指定文件的文本内容（原逻辑不变，不依赖 apk-parser）
     */
    suspend fun getInternalFileContent(apkFile: File, entryPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                ZipFile(apkFile).use { zip ->
                    val entry = zip.getEntry(entryPath)
                        ?: throw IllegalStateException("找不到文件: $entryPath")
                    zip.getInputStream(entry).use { input ->
                        val bytes = input.readBytes()
                        if (isText(bytes)) {
                            String(bytes, Charsets.UTF_8)
                        } else {
                            ""
                        }
                    }
                }
            }
        }

    /**
     * 判断字节数组是否为文本（原逻辑不变）
     */
    private fun isText(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true
        var textCount = 0
        for (b in bytes) {
            val i = b.toInt() and 0xFF
            if (i in 0x20..0x7E || i == 0x0A || i == 0x0D || i == 0x09) {
                textCount++
            } else if (i > 0x7F) {
                textCount++
            }
        }
        return textCount > bytes.size * 0.8
    }
}