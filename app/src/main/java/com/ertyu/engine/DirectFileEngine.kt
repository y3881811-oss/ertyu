package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import com.ertyu.model.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
        val dir = uri.toFileOrNull()?.takeIf { it.isDirectory } ?: return@withContext emptyList()
        val children = dir.listFiles() ?: throw SecurityException("无权限访问此目录")
        val result = ArrayList<FileItem>(children.size)
        for (file in children) {
            val isDir = file.isDirectory
            val size = if (isDir) 0L else file.length()
            val childUri = Uri.fromFile(file)
            val lastMod = file.lastModified()
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
                    lastModifiedText = if (lastMod > 0) dateFormat.format(Date(lastMod)) else "未知",
                    childCount = childCount
                )
            )
        }
        result
    }

    // ==================== 带进度的复制与移动 ====================

    override suspend fun copy(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val src = source.toFileOrNull() ?: return@withContext false
            val dir = targetDir.toFileOrNull() ?: return@withContext false
            
            try {
                val totalBytes = getFileSize(src)
                var copiedBytes = 0L
                var startTime = System.currentTimeMillis()
                var lastUpdateTime = startTime
                var lastCopiedBytes = 0L

                fun copyRecursiveWithProgress(s: File, d: File) {
                    if (s.isDirectory) {
                        d.mkdirs()
                        s.listFiles()?.forEach { copyRecursiveWithProgress(it, File(d, it.name)) }
                    } else {
                        d.parentFile?.mkdirs()
                        FileInputStream(s).use { input ->
                            FileOutputStream(d).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var len: Int
                                while (input.read(buffer).also { len = it } != -1) {
                                    output.write(buffer, 0, len)
                                    copiedBytes += len
                                    
                                    val now = System.currentTimeMillis()
                                    if (now - lastUpdateTime > 200) { // 200ms 刷新一次 UI，防止卡顿
                                        val speed = ((copiedBytes - lastCopiedBytes) * 1000) / (now - lastUpdateTime)
                                        onProgress(TransferProgress(copiedBytes, totalBytes, s.name, speed))
                                        lastUpdateTime = now
                                        lastCopiedBytes = copiedBytes
                                    }
                                }
                            }
                        }
                    }
                }

                val destFile = File(dir, src.name)
                copyRecursiveWithProgress(src, destFile)
                onProgress(TransferProgress(totalBytes, totalBytes, src.name, 0L)) // 完成
                true
            } catch (e: Exception) {
                false
            }
        }

    override suspend fun move(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean =
        withContext(Dispatchers.IO) {
            val src = source.toFileOrNull() ?: return@withContext false
            val dir = targetDir.toFileOrNull() ?: return@withContext false
            val dst = File(dir, src.name)
            
            // 尝试直接重命名（同分区秒完成）
            if (src.renameTo(dst)) {
                onProgress(TransferProgress(1L, 1L, src.name, 0L))
                return@withContext true
            }
            
            // 跨分区，降级为复制+删除，并显示进度
            val ok = copy(source, targetDir, onProgress)
            if (ok) src.deleteRecursively()
            ok
        }

    private fun getFileSize(file: File): Long {
        if (file.isFile) return file.length()
        var size = 0L
        file.listFiles()?.forEach { size += getFileSize(it) }
        return size
    }

    // ==================== 其他基础操作 ====================

    override suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        uri.toFileOrNull()?.deleteRecursively() ?: false
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val parentFile = parent.toFileOrNull() ?: return@withContext null
        val dir = File(parentFile, name)
        if (dir.mkdirs()) Uri.fromFile(dir) else null
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val src = uri.toFileOrNull() ?: return@withContext null
        val dst = File(src.parentFile, newName)
        if (src.renameTo(dst)) Uri.fromFile(dst) else null
    }

    override fun getParent(uri: Uri): Uri? = uri.toFileOrNull()?.parentFile?.let { Uri.fromFile(it) }
    override fun getDisplayName(uri: Uri): String = uri.toFileOrNull()?.name ?: uri.lastPathSegment ?: ""

    override suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        val file = uri.toFileOrNull() ?: return@withContext null
        if (!file.isFile) return@withContext null
        runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    override suspend fun writeText(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        val file = uri.toFileOrNull() ?: return@withContext false
        runCatching { file.parentFile?.mkdirs(); file.writeText(content, Charsets.UTF_8) }.isSuccess
    }

    private fun guessType(file: File): String {
        if (file.isDirectory) return "文件夹"
        val ext = file.name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "log", "ini", "conf", "cfg" -> "文本"
            "md" -> "Markdown"
            "json" -> "JSON"
            "xml" -> "XML"
            "jpg", "jpeg", "png", "gif", "bmp", "webp" -> "图片"
            "mp3", "wav", "flac", "ogg", "m4a", "aac" -> "音频"
            "mp4", "mkv", "avi", "mov", "webm", "flv" -> "视频"
            "zip", "rar", "7z", "tar", "gz", "apk" -> "压缩包/应用"
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
}