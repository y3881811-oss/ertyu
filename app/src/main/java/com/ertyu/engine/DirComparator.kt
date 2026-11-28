package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs

/** 目录对比结果的类型 */
enum class DiffKind(val label: String) {
    LEFT_ONLY("仅左侧"),
    RIGHT_ONLY("仅右侧"),
    DIFFERENT("内容不同"),
    SAME("相同")
}

data class DirDiffEntry(
    val relPath: String,
    val kind: DiffKind,
    val isDirectory: Boolean,
    val leftSize: Long,
    val rightSize: Long,
    val leftTime: Long,
    val rightTime: Long,
    val leftPath: String,
    val rightPath: String
)

/**
 * 双面板目录对比与同步。
 * 递归建立相对路径 -> (大小, 修改时间, 是否目录) 的索引，再逐一比对。
 */
object DirComparator {

    data class Node(val size: Long, val time: Long, val isDir: Boolean)

    private suspend fun collect(root: File, out: MutableMap<String, Node>) {
        val rootPath = root.absolutePath
        suspend fun walk(dir: File) {
            currentCoroutineContext().ensureActive()
            val children = try {
                dir.listFiles()
            } catch (_: Exception) {
                null
            } ?: return
            for (c in children) {
                val rel = c.absolutePath.removePrefix(rootPath).trimStart('/')
                if (rel.isEmpty()) continue
                out[rel] = Node(if (c.isDirectory) 0L else c.length(), c.lastModified(), c.isDirectory)
                if (c.isDirectory) walk(c)
            }
        }
        walk(root)
    }

    suspend fun compare(
        left: File,
        right: File,
        deep: Boolean = true,
        onProgress: suspend (leftCount: Int, rightCount: Int) -> Unit = { _, _ -> }
    ): List<DirDiffEntry> = withContext(Dispatchers.IO) {
        val l = HashMap<String, Node>()
        val r = HashMap<String, Node>()
        collect(left, l)
        onProgress(l.size, 0)
        collect(right, r)
        onProgress(l.size, r.size)

        val keys = sortedSetOf<String>()
        keys.addAll(l.keys)
        keys.addAll(r.keys)

        val result = ArrayList<DirDiffEntry>(keys.size)
        for (k in keys) {
            val a = l[k]
            val b = r[k]
            val kind = when {
                a == null -> DiffKind.RIGHT_ONLY
                b == null -> DiffKind.LEFT_ONLY
                a.isDir != b.isDir -> DiffKind.DIFFERENT
                a.isDir -> DiffKind.SAME
                !deep -> DiffKind.SAME
                a.size != b.size -> DiffKind.DIFFERENT
                abs(a.time - b.time) > 2000L -> DiffKind.DIFFERENT
                else -> DiffKind.SAME
            }
            result.add(
                DirDiffEntry(
                    relPath = k,
                    kind = kind,
                    isDirectory = a?.isDir ?: b?.isDir ?: false,
                    leftSize = a?.size ?: 0L,
                    rightSize = b?.size ?: 0L,
                    leftTime = a?.time ?: 0L,
                    rightTime = b?.time ?: 0L,
                    leftPath = left.absolutePath + "/" + k,
                    rightPath = right.absolutePath + "/" + k
                )
            )
        }
        result
    }

    /** 直接删除一组路径（镜像模式用，调用方负责回收站策略）。 */
    suspend fun deletePaths(
        paths: List<String>,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var ok = 0
        var fail = 0
        var done = 0
        for (p in paths) {
            currentCoroutineContext().ensureActive()
            val f = File(p)
            val success = try {
                when {
                    !f.exists() -> true
                    f.isDirectory -> f.deleteRecursively()
                    else -> f.delete()
                }
            } catch (_: Exception) {
                false
            }
            if (success) ok++ else fail++
            done++
            onProgress(done, paths.size)
        }
        ok to fail
    }

    /**
     * 把选中的差异项复制到另一侧。
     * toRight=true：左 -> 右；false：右 -> 左。返回 (成功数, 失败数)。
     */
    suspend fun copyTo(
        entries: List<DirDiffEntry>,
        toRight: Boolean,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var ok = 0
        var fail = 0
        var done = 0
        for (e in entries) {
            currentCoroutineContext().ensureActive()
            if (e.isDirectory) {
                // 目录本身没有内容可复制，建出来即可
                val dir = File(if (toRight) File(e.rightPath).absolutePath else e.leftPath)
                try {
                    if (dir.mkdirs() || dir.isDirectory) ok++ else fail++
                } catch (_: Exception) {
                    fail++
                }
                done++
                onProgress(done, entries.size)
                continue
            }
            val src = File(if (toRight) e.leftPath else e.rightPath)
            val dst = File(if (toRight) e.rightPath else e.leftPath)
            try {
                if (!src.exists()) {
                    fail++
                } else {
                    dst.parentFile?.mkdirs()
                    src.copyTo(dst, overwrite = true)
                    ok++
                }
            } catch (_: Exception) {
                fail++
            }
            done++
            onProgress(done, entries.size)
        }
        ok to fail
    }
}
