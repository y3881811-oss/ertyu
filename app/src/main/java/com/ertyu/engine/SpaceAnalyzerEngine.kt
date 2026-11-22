package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.util.PriorityQueue

/**
 * 空间分析：一次遍历同时得到
 * 1. 最大的 N 个文件
 * 2. 根目录下各一级子目录的占用排行
 * 3. 总文件数 / 总字节数
 */
object SpaceAnalyzerEngine {

    data class BigFile(val path: String, val size: Long, val modified: Long)
    data class FolderUsage(val path: String, val bytes: Long, val fileCount: Int)
    data class Result(
        val bigFiles: List<BigFile>,
        val folders: List<FolderUsage>,
        val totalBytes: Long,
        val totalFiles: Int
    )

    suspend fun scan(
        root: File,
        minSizeBytes: Long,
        topFileCount: Int = 100,
        topFolderCount: Int = 40,
        onProgress: suspend (files: Int, bytes: Long) -> Unit = { _, _ -> }
    ): Result = withContext(Dispatchers.IO) {
        // 小顶堆：只保留最大的 topFileCount 个
        val heap = PriorityQueue<BigFile>(topFileCount.coerceAtLeast(1)) { a, b -> a.size.compareTo(b.size) }
        val folderBytes = HashMap<String, Long>()
        val folderFiles = HashMap<String, Int>()

        var totalBytes = 0L
        var totalFiles = 0
        var lastEmit = 0L
        val rootPath = root.absolutePath

        suspend fun topFolderOf(path: String): String? {
            if (path.length <= rootPath.length + 1) return null
            val rel = path.substring(rootPath.length).trimStart('/')
            val first = rel.substringBefore('/')
            if (first.isEmpty() || !rel.contains('/')) return null
            return "$rootPath/$first"
        }

        suspend fun walk(dir: File) {
            val children = try {
                dir.listFiles()
            } catch (_: Exception) {
                null
            } ?: return

            for (child in children) {
                if (child.isDirectory) {
                    walk(child)
                } else {
                    val len = try {
                        child.length()
                    } catch (_: Exception) {
                        0L
                    }
                    totalBytes += len
                    totalFiles++

                    if (len >= minSizeBytes) {
                        val bf = BigFile(child.absolutePath, len, try { child.lastModified() } catch (_: Exception) { 0L })
                        if (heap.size < topFileCount) {
                            heap.add(bf)
                        } else {
                            val smallest = heap.peek()
                            if (smallest != null && len > smallest.size) {
                                heap.poll()
                                heap.add(bf)
                            }
                        }
                    }

                    val top = topFolderOf(child.absolutePath)
                    if (top != null) {
                        folderBytes[top] = (folderBytes[top] ?: 0L) + len
                        folderFiles[top] = (folderFiles[top] ?: 0) + 1
                    }

                    if (totalFiles and 0x1FF == 0) {
                        currentCoroutineContext().ensureActive()
                        val now = System.currentTimeMillis()
                        if (now - lastEmit > 250) {
                            lastEmit = now
                            onProgress(totalFiles, totalBytes)
                        }
                    }
                }
            }
        }

        walk(root)
        onProgress(totalFiles, totalBytes)

        val bigFiles = heap.toList().sortedByDescending { it.size }
        val folders = folderBytes.entries
            .map { FolderUsage(it.key, it.value, folderFiles[it.key] ?: 0) }
            .sortedByDescending { it.bytes }
            .take(topFolderCount)

        Result(bigFiles, folders, totalBytes, totalFiles)
    }
}
