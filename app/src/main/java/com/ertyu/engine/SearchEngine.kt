package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 并行递归搜索。和 FolderSizeCalculator 一样：
 * - 8 路并发
 * - 只在写共享变量时加锁
 * - 每 100 个文件汇报一次
 * - 结果超过 maxResults 时停止
 */
object SearchEngine {

    private const val CONCURRENCY = 8
    private const val REPORT_INTERVAL = 100
    private const val MAX_RESULTS = 1000

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    suspend fun search(
        root: File,
        query: String,
        onProgress: (dirs: Int, files: Int, results: List<FileItem>) -> Unit
    ): Pair<List<FileItem>, Boolean> =
        withContext(Dispatchers.IO.limitedParallelism(CONCURRENCY)) {
            if (query.isBlank()) return@withContext emptyList<FileItem>() to false

            val mutex = Mutex()
            var dirsCount = 0
            var filesCount = 0
            var lastReported = 0
            val results = ArrayList<FileItem>()
            var truncated = false

            suspend fun walk(dir: File) {
                val shouldStop = mutex.withLock { truncated }
                if (shouldStop) return

                val children = try {
                    dir.listFiles()
                } catch (e: Exception) {
                    null
                } ?: return

                mutex.withLock { dirsCount++ }

                val subDirs = ArrayList<File>(children.size)
                for (child in children) {
                    val name = child.name
                    if (name.contains(query, ignoreCase = true)) {
                        mutex.withLock {
                            if (results.size < MAX_RESULTS) {
                                results.add(toFileItem(child))
                            } else {
                                truncated = true
                            }
                        }
                    }
                    if (child.isDirectory) {
                        subDirs.add(child)
                    } else {
                        mutex.withLock { filesCount++ }
                    }
                }

                // 批量汇报
                var shouldReport = false
                var snapDirs = 0
                var snapFiles = 0
                var snapResults: List<FileItem> = emptyList()
                mutex.withLock {
                    if (filesCount - lastReported >= REPORT_INTERVAL) {
                        lastReported = filesCount
                        shouldReport = true
                        snapDirs = dirsCount
                        snapFiles = filesCount
                        snapResults = ArrayList(results)
                    }
                }
                if (shouldReport) {
                    onProgress(snapDirs, snapFiles, snapResults)
                }

                when {
                    subDirs.size == 1 -> walk(subDirs[0])
                    subDirs.size > 1 -> coroutineScope {
                        for (sub in subDirs) launch { walk(sub) }
                    }
                }
            }

            walk(root)
            onProgress(dirsCount, filesCount, ArrayList(results))
            ArrayList(results) to truncated
        }

    private fun toFileItem(file: File): FileItem {
        val isDir = file.isDirectory
        val size = if (isDir) 0L else file.length()
        val childUri = Uri.fromFile(file)
        val lastMod = file.lastModified()
        return FileItem(
            name = file.name,
            uri = childUri,
            isDirectory = isDir,
            size = size,
            lastModified = lastMod,
            mimeType = null,
            uriString = childUri.toString(),
            path = file.absolutePath,
            sizeText = if (isDir) "" else formatSize(size),
            typeText = if (isDir) "文件夹" else "文件",
            lastModifiedText = if (lastMod > 0)
                dateFormat.format(Date(lastMod)) else "未知",
            childCount = -1
        )
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