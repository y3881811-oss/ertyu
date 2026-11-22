package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 目录大小的“轻量”计算器：带内存缓存、并发限制和文件数上限。
 * 与已有的 FolderSizeCalculator 并存，互不修改。
 */
object ItemSizeEngine {

    private const val CONCURRENCY = 3

    /** 单个目录最多统计 12 万个文件，超出就标记截断，避免卡死手机 */
    private const val MAX_FILES = 120_000

    /** 缓存有效期 10 分钟 */
    private const val CACHE_TTL_MS = 10 * 60 * 1000L

    data class Result(val bytes: Long, val files: Int, val truncated: Boolean)

    private class CacheEntry(val bytes: Long, val files: Int, val truncated: Boolean, val at: Long)

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    /** 这些目录过于庞大或属于系统目录，自动计算时直接跳过 */
    private val SKIP_PREFIX = listOf(
        "/storage/emulated/0/Android/data",
        "/storage/emulated/0/Android/obb",
        "/storage/emulated/0/Android/media",
        "/data",
        "/system",
        "/proc"
    )

    fun shouldSkip(path: String): Boolean =
        path.isBlank() || SKIP_PREFIX.any { path == it || path.startsWith("$it/") }

    fun cached(path: String): Result? {
        val e = cache[path] ?: return null
        if (System.currentTimeMillis() - e.at > CACHE_TTL_MS) {
            cache.remove(path)
            return null
        }
        return Result(e.bytes, e.files, e.truncated)
    }

    fun invalidate(path: String? = null) {
        if (path == null) cache.clear() else cache.remove(path)
    }

    suspend fun calculate(root: File): Result =
        withContext(Dispatchers.IO.limitedParallelism(CONCURRENCY)) {
            val mutex = Mutex()
            var bytes = 0L
            var count = 0
            var truncated = false

            suspend fun walk(dir: File) {
                if (truncated) return
                val children = try {
                    dir.listFiles()
                } catch (_: Exception) {
                    null
                } ?: return

                val subs = ArrayList<File>(children.size)
                for (child in children) {
                    if (child.isDirectory) {
                        subs.add(child)
                    } else {
                        val len = try {
                            child.length()
                        } catch (_: Exception) {
                            0L
                        }
                        mutex.withLock {
                            bytes += len
                            count++
                        }
                    }
                }

                mutex.withLock {
                    if (count >= MAX_FILES) truncated = true
                }
                if (truncated) return

                if (subs.isNotEmpty()) {
                    coroutineScope {
                        for (s in subs) {
                            launch {
                                currentCoroutineContext().ensureActive()
                                walk(s)
                            }
                        }
                    }
                }
            }

            walk(root)
            val result = Result(bytes, count, truncated)
            cache[root.absolutePath] = CacheEntry(result.bytes, result.files, result.truncated, System.currentTimeMillis())
            result
        }

    fun format(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format(Locale.ROOT, "%.1f MB", mb)
        val gb = mb / 1024.0
        if (gb < 1024) return String.format(Locale.ROOT, "%.2f GB", gb)
        return String.format(Locale.ROOT, "%.2f TB", gb / 1024.0)
    }
}
