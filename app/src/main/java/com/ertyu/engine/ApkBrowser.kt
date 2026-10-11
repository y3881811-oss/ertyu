package com.ertyu.engine

import com.ertyu.model.ApkEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ApkBrowser {

    suspend fun list(apkFile: File): Result<List<ApkEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!apkFile.exists()) {
                    throw IllegalArgumentException("APK 文件不存在")
                }
                val result = ArrayList<ApkEntry>()
                ZipFile(apkFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val e = entries.nextElement()
                        val path = e.name
                        val isDir = e.isDirectory
                        val name = if (isDir)
                            path.trimEnd('/').substringAfterLast('/')
                        else
                            path.substringAfterLast('/')
                        val depth = path.trimEnd('/').count { it == '/' }
                        val size = if (isDir) 0L else e.size.coerceAtLeast(0)
                        val compressed = if (isDir) 0L else e.compressedSize.coerceAtLeast(0)
                        result.add(
                            ApkEntry(
                                path = path,
                                name = name,
                                isDirectory = isDir,
                                size = size,
                                compressedSize = compressed,
                                sizeText = if (isDir) "" else formatSize(size),
                                compressedText = if (isDir) "" else formatSize(compressed),
                                depth = depth,
                                isDex = !isDir && name.startsWith("classes") && name.endsWith(".dex")
                            )
                        )
                    }
                }
                result.sortWith(
                    compareBy<ApkEntry> { it.path.lowercase() }
                )
                result
            }
        }

    /** 提取 APK 内单个文件到指定目录（保持原文件名） */
    suspend fun extractEntry(
        apkFile: File,
        entryPath: String,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!targetDir.exists()) targetDir.mkdirs()
            val outFile = File(targetDir, entryPath.substringAfterLast('/'))
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry(entryPath)
                    ?: throw IllegalArgumentException("找不到条目：$entryPath")
                zip.getInputStream(entry).use { input ->
                    FileOutputStream(outFile).use { out ->
                        input.copyTo(out, 16 * 1024)
                    }
                }
            }
            outFile
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}