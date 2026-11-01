package com.ertyu.engine

import android.util.Log
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ApkParserEngine {
    private const val TAG = "ApkParserEngine"

    suspend fun getManifest(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val xmlString = ApkModule.loadApkFile(apkFile).use { apkModule ->
                val manifestBlock: AndroidManifestBlock = apkModule.androidManifest
                    ?: throw IllegalStateException("APK中不包含 AndroidManifest.xml")
                manifestBlock.serializeToXml()
            }
            if (xmlString.isNullOrBlank()) {
                Result.failure(Exception("解析出的 Manifest 为空"))
            } else {
                Result.success(xmlString)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "解析 Manifest 失败", e)
            Result.failure(Exception("解析异常: ${e.message}"))
        }
    }

    suspend fun getResourcesArsc(apkFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val html = ApkModule.loadApkFile(apkFile).use { apkModule ->
                val manifestBlock: AndroidManifestBlock = apkModule.androidManifest
                    ?: throw IllegalStateException("无法获取 AndroidManifestBlock")

                val packageName = manifestBlock.packageName ?: "未知"
                val versionName = manifestBlock.versionName ?: "未知"
                val versionCode = manifestBlock.versionCode?.toString() ?: "0"
                val minSdk = manifestBlock.minSdkVersion?.toString() ?: "0"
                val targetSdk = manifestBlock.targetSdkVersion?.toString() ?: "0"
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
                    permissions.forEach { perm -> sb.appendLine("<p>$perm</p>") }
                }
                sb.toString()
            }
            Result.success(html)
        } catch (e: Throwable) {
            Log.e(TAG, "解析 resources.arsc 失败", e)
            Result.failure(Exception("解析异常: ${e.message}"))
        }
    }

    suspend fun getInternalFileContent(apkFile: File, entryPath: String): Result<String> =
        withContext(Dispatchers.IO) {
            // 特殊处理 Manifest
            if (entryPath == "AndroidManifest.xml") {
                return@withContext getManifest(apkFile)
            }
            
            try {
                val apkModule = ApkModule.loadApkFile(apkFile)
                try {
                    // 修复：直接使用 getInputSource(entryPath) 获取内部文件流
                    val inputSource = apkModule.getInputSource(entryPath)
                        ?: throw IllegalStateException("找不到内部文件: $entryPath")

                    if (entryPath.endsWith(".xml", ignoreCase = true) || 
                        ResXmlDocument.isResXmlBlock(inputSource)) {
                        val xmlDoc = ResXmlDocument()
                        inputSource.openStream().use { stream ->
                            xmlDoc.readBytes(stream)
                        }
                        Result.success(xmlDoc.serializeToXml())
                    } else {
                        inputSource.openStream().use { stream ->
                            val bytes = stream.readBytes()
                            if (isText(bytes)) {
                                Result.success(String(bytes, Charsets.UTF_8))
                            } else {
                                Result.success("该文件为二进制文件，无法直接预览。")
                            }
                        }
                    }
                } finally {
                    // 修复：手动关闭 apkModule，避免 use 嵌套导致类型推断失败
                    apkModule.close()
                }
            } catch (e: Throwable) {
                Log.e(TAG, "读取内部文件 $entryPath 失败", e)
                Result.failure(Exception("读取失败: ${e.message}"))
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