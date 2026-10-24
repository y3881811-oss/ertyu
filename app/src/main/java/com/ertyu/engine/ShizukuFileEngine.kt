package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuRemoteProcess
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShizukuFileEngine : FileEngine {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun sh(cmd: String): String {
        if (!Shizuku.pingBinder()) return ""
        return try {
            // 使用反射调用因为 13.x 版本将其标记为 private
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? ShizukuRemoteProcess
            if (process != null) {
                val output = process.inputStream.bufferedReader().readText()
                process.waitFor()
                output
            } else {
                ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    override suspend fun list(uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext emptyList()
        val raw = sh("ls -la '$path'")
        if (raw.isEmpty()) return@withContext emptyList()
        
        val lines = raw.split("\n")
        val result = ArrayList<FileItem>()
        
        for (line in lines) {
            val parts = line.trim().split(Regex("\\s+"), limit = 7)
            if (parts.size < 7) continue
            val perms = parts[0]
            val size = parts[4].toLongOrNull() ?: 0L
            val name = parts[6]
            if (name == "." || name == "..") continue
            
            val isDir = perms.startsWith("d")
            val childPath = if (path.endsWith("/")) "$path$name" else "$path/$name"
            val childUri = Uri.fromFile(File(childPath))
            
            result.add(
                FileItem(
                    name = name,
                    uri = childUri,
                    isDirectory = isDir,
                    size = size,
                    lastModified = 0,
                    uriString = childUri.toString(),
                    path = childPath,
                    sizeText = if (isDir) "" else formatSize(size),
                    typeText = if (isDir) "文件夹" else "文件",
                    lastModifiedText = "未知",
                    childCount = -1
                )
            )
        }
        result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    override suspend fun delete(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext false
        sh("rm -rf '$path'")
        true
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val path = parent.path ?: return@withContext null
        val newPath = "$path/$name"
        sh("mkdir -p '$newPath'")
        Uri.fromFile(File(newPath))
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext null
        val dir = File(path).parent ?: return@withContext null
        val newPath = "$dir/$newName"
        sh("mv '$path' '$newPath'")
        Uri.fromFile(File(newPath))
    }

    override suspend fun copy(source: Uri, targetDir: Uri): Boolean = withContext(Dispatchers.IO) {
        val src = source.path ?: return@withContext false
        val dst = targetDir.path ?: return@withContext false
        val name = File(src).name
        sh("cp -r '$src' '$dst/$name'")
        true
    }

    override suspend fun move(source: Uri, targetDir: Uri): Boolean = withContext(Dispatchers.IO) {
        val src = source.path ?: return@withContext false
        val dst = targetDir.path ?: return@withContext false
        val name = File(src).name
        sh("mv '$src' '$dst/$name'")
        true
    }

    override fun getParent(uri: Uri): Uri? {
        val path = uri.path ?: return null
        val parent = File(path).parent ?: return null
        return Uri.fromFile(File(parent))
    }

    override fun getDisplayName(uri: Uri): String = uri.path?.let { File(it).name } ?: ""

    override suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext null
        sh("cat '$path'")
    }

    override suspend fun writeText(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext false
        val escaped = content.replace("'", "'\\''")
        sh("echo '$escaped' > '$path'")
        true
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