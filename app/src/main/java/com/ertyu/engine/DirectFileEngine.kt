package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DirectFileEngine : FileEngine {

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
            result.add(
                FileItem(
                    name = file.name,
                    uri = childUri,
                    isDirectory = isDir,
                    size = size,
                    lastModified = file.lastModified(),
                    mimeType = null,
                    uriString = childUri.toString(),
                    sizeText = if (isDir) "" else formatSize(size)
                )
            )
        }
        result
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