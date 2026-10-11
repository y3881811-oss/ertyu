package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DirectFileEngine : FileEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun Uri.toFileOrNull(): File? = when (scheme) {
        "file" -> path?.let(::File)
        null -> path?.let(::File)
        else -> null
    }

    override suspend fun list(uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val dir = uri.toFileOrNull()?.takeIf { it.isDirectory }
            ?: return@withContext emptyList()

        val children = dir.listFiles() ?: return@withContext emptyList()
        val result = ArrayList<FileItem>(children.size)
        for (file in children) {
            val isDir = file.isDirectory
            val size = if (isDir) 0L else file.length()
            val childUri = Uri.fromFile(file)
            val lastMod = file.lastModified()
            // 目录：统计子项数（一次轻量 listFiles，不再递归）
            val childCount = if (isDir) (file.listFiles()?.size ?: 0) else -1
            result.add(
                FileItem(
                    name = file.name,
                    uri = childUri,
                    isDirectory = isDir,
                    size = size,
                    lastModified = lastMod,
                    mimeType = null,
                    uriString = childUri.toString(),
                    path = file.absolutePath,
                    sizeText = if (isDir) "" else formatSize(size),
                    typeText = guessType(file),
                    lastModifiedText = if (lastMod > 0)
                        dateFormat.format(Date(lastMod)) else "未知",
                    childCount = childCount
                )
            )
        }
        result
    }

    private fun guessType(file: File): String {
        if (file.isDirectory) return "文件夹"
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "log", "ini", "conf", "cfg" -> "文本"
            "md" -> "Markdown"
            "json" -> "JSON"
            "xml" -> "XML"
            "html", "htm" -> "网页"
            "css" -> "样式表"
            "js" -> "JavaScript"
            "kt", "kts" -> "Kotlin 代码"
            "java" -> "Java 代码"
            "gradle" -> "Gradle 脚本"
            "properties" -> "属性文件"
            "yml", "yaml" -> "YAML"
            "sh", "bat" -> "脚本"
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "图片"
            "mp3", "wav", "flac", "ogg", "m4a", "aac" -> "音频"
            "mp4", "mkv", "avi", "mov", "webm", "flv" -> "视频"
            "zip" -> "ZIP 压缩包"
            "rar" -> "RAR 压缩包"
            "7z" -> "7Z 压缩包"
            "tar" -> "TAR 归档"
            "gz" -> "GZ 压缩"
            "apk" -> "Android 应用"
            "pdf" -> "PDF 文档"
            "doc", "docx" -> "Word 文档"
            "xls", "xlsx" -> "Excel 表格"
            "ppt", "pptx" -> "PowerPoint"
            "ttf", "otf" -> "字体"
            "db", "sqlite" -> "数据库"
            else -> if (ext.isEmpty()) "文件" else "$ext 文件"
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

    override suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        uri.toFileOrNull()?.deleteRecursively() ?: false
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? =
        withContext(Dispatchers.IO) {
            val parentFile = parent.toFileOrNull() ?: return@withContext null
            val dir = File(parentFile, name)
            if (dir.mkdirs()) Uri.fromFile(dir) else null
        }

    override suspend fun rename(uri: Uri, newName: String): Uri? =
        withContext(Dispatchers.IO) {
            val src = uri.toFileOrNull() ?: return@withContext null
            val dst = File(src.parentFile, newName)
            if (src.renameTo(dst)) Uri.fromFile(dst) else null
        }

    override suspend fun copy(source: Uri, targetDir: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val src = source.toFileOrNull() ?: return@withContext false
            val dir = targetDir.toFileOrNull() ?: return@withContext false
            runCatching { copyRecursively(src, File(dir, src.name)) }.isSuccess
        }

    private fun copyRecursively(src: File, dst: File) {
        if (src.isDirectory) {
            dst.mkdirs()
            src.listFiles()?.forEach { copyRecursively(it, File(dst, it.name)) }
        } else {
            dst.parentFile?.mkdirs()
            src.copyTo(dst, overwrite = true)
        }
    }

    override suspend fun move(source: Uri, targetDir: Uri): Boolean =
        withContext(Dispatchers.IO) {
            val src = source.toFileOrNull() ?: return@withContext false
            val dir = targetDir.toFileOrNull() ?: return@withContext false
            val dst = File(dir, src.name)
            if (src.renameTo(dst)) return@withContext true
            val ok = copy(source, targetDir)
            if (ok) src.deleteRecursively()
            ok
        }

    override fun getParent(uri: Uri): Uri? =
        uri.toFileOrNull()?.parentFile?.let { Uri.fromFile(it) }

    override fun getDisplayName(uri: Uri): String =
        uri.toFileOrNull()?.name ?: uri.lastPathSegment ?: ""

    override suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        val file = uri.toFileOrNull() ?: return@withContext null
        if (!file.isFile) return@withContext null
        runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    override suspend fun writeText(uri: Uri, content: String): Boolean =
        withContext(Dispatchers.IO) {
            val file = uri.toFileOrNull() ?: return@withContext false
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(content, Charsets.UTF_8)
            }.isSuccess
        }
}