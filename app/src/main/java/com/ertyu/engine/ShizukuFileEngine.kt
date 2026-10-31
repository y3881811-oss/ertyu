package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import com.ertyu.model.TransferProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ShizukuFileEngine : FileEngine {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun sh(cmd: String): String = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                if (!Shizuku.pingBinder()) return@withContext "Shizuku 服务未运行"
                
                // 使用反射调用 Shizuku 的私有方法 newProcess 来执行高权限命令
                val method = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                method.isAccessible = true
                val process = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as Process
                
                val output = process.inputStream.bufferedReader().readText()
                val error = process.errorStream.bufferedReader().readText()
                process.waitFor()
                
                if (error.isNotBlank()) "Error: $error" else output
            } catch (e: Exception) {
                "执行异常: ${e.message}"
            }
        }
    }

    private fun Uri.toFileOrNull(): File? = path?.let(::File)

    override suspend fun list(uri: Uri): List<FileItem> = withContext(Dispatchers.IO) {
        val path = uri.path ?: return@withContext emptyList()
        val raw = sh("ls -la '$path'")
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
        val p = uri.path ?: return@withContext false
        sh("rm -rf '$p'")
        true
    }

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withContext(Dispatchers.IO) {
        val p = parent.path ?: return@withContext null
        val newPath = "$p/$name"
        sh("mkdir -p '$newPath'")
        Uri.fromFile(File(newPath))
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withContext(Dispatchers.IO) {
        val p = uri.path ?: return@withContext null
        val d = File(p).parent ?: return@withContext null
        val newPath = "$d/$newName"
        sh("mv '$p' '$newPath'")
        Uri.fromFile(File(newPath))
    }

    override suspend fun copy(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val s = source.path ?: return@withContext false
        val d = targetDir.path ?: return@withContext false
        val name = File(s).name
        sh("cp -r '$s' '$d/$name'")
        true
    }

    override suspend fun move(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val s = source.path ?: return@withContext false
        val d = targetDir.path ?: return@withContext false
        val name = File(s).name
        sh("mv '$s' '$d/$name'")
        true
    }

    override fun getParent(uri: Uri): Uri? {
        val p = uri.path ?: return null
        val parent = File(p).parent ?: return null
        return Uri.fromFile(File(parent))
    }

    override fun getDisplayName(uri: Uri): String = uri.path?.let { File(it).name } ?: ""

    override suspend fun readText(uri: Uri): String? = withContext(Dispatchers.IO) {
        val p = uri.path ?: return@withContext null
        sh("cat '$p'")
    }

    override suspend fun writeText(uri: Uri, content: String): Boolean = withContext(Dispatchers.IO) {
        val p = uri.path ?: return@withContext false
        val escaped = content.replace("'", "'\\''")
        sh("echo '$escaped' > '$p'")
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