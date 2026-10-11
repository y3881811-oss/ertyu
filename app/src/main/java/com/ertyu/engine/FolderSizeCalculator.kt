package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 并行计算文件夹总大小。
 *
 * 加速原理：
 * 1. 用 limitedParallelism(8) 限制同时执行的协程数，避免线程爆炸
 * 2. 只在累加共享变量时加锁，遍历目录不加锁
 * 3. 每 200 个文件回调一次进度，减少 UI 重组
 */
object FolderSizeCalculator {

    private const val CONCURRENCY = 8
    private const val REPORT_INTERVAL = 200

    suspend fun calculate(
        root: File,
        onProgress: (scanned: Int, totalBytes: Long) -> Unit
    ): Pair<Long, Int> = withContext(Dispatchers.IO.limitedParallelism(CONCURRENCY)) {
        val mutex = Mutex()
        var totalBytes = 0L
        var fileCount = 0
        var lastReported = 0

        suspend fun walk(dir: File) {
            val children = try {
                dir.listFiles()
            } catch (e: Exception) {
                null
            } ?: return

            val subDirs = ArrayList<File>(children.size)
            for (child in children) {
                if (child.isDirectory) {
                    subDirs.add(child)
                } else {
                    val len = child.length()
                    mutex.withLock {
                        totalBytes += len
                        fileCount++
                    }
                }
            }

            // 批量报告进度
            var shouldReport = false
            var snapCount = 0
            var snapBytes = 0L
            mutex.withLock {
                if (fileCount - lastReported >= REPORT_INTERVAL) {
                    lastReported = fileCount
                    shouldReport = true
                    snapCount = fileCount
                    snapBytes = totalBytes
                }
            }
            if (shouldReport) {
                onProgress(snapCount, snapBytes)
            }

            // 并行处理子目录
            if (subDirs.isNotEmpty()) {
                coroutineScope {
                    for (sub in subDirs) {
                        launch { walk(sub) }
                    }
                }
            }
        }

        walk(root)
        onProgress(fileCount, totalBytes)
        totalBytes to fileCount
    }
}