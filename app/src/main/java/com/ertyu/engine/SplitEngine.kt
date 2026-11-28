package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.util.Locale

/**
 * 分卷 / 合并。
 * 分卷产物命名：原名.001、原名.002 ……（合并时按序号拼接还原）。
 */
object SplitEngine {

    private val PART_REGEX = Regex("""^(.+)\.(\d{3})$""")

    fun isPart(name: String): Boolean = PART_REGEX.matches(name)

    /** 分卷；返回生成的卷列表 */
    suspend fun split(
        source: File,
        partSizeBytes: Long,
        onProgress: (Float) -> Unit = {}
    ): Result<List<File>> = withContext(Dispatchers.IO) {
        runCatching {
            require(source.isFile) { "只能对文件分卷（文件夹请先压缩）" }
            require(partSizeBytes > 0) { "分卷大小无效" }
            val total = source.length()
            if (total <= 0) return@runCatching emptyList()

            val outputs = ArrayList<File>()
            RandomAccessFile(source, "r").use { raf ->
                var index = 1
                var offset = 0L
                val buf = ByteArray(256 * 1024)
                while (offset < total) {
                    val partFile = File(source.parentFile, String.format(Locale.ROOT, "%s.%03d", source.name, index))
                    val limit = minOf(partSizeBytes, total - offset)
                    partFile.outputStream().use { out ->
                        var written = 0L
                        while (written < limit) {
                            val want = minOf(buf.size.toLong(), limit - written).toInt()
                            val read = raf.read(buf, 0, want)
                            if (read <= 0) break
                            out.write(buf, 0, read)
                            written += read
                        }
                    }
                    offset += limit
                    outputs.add(partFile)
                    index++
                    onProgress((offset.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                }
            }
            AppLogger.i("Split", "分卷完成：${source.name} → ${outputs.size} 卷")
            outputs
        }
    }

    /** 按序号合并；parts 会按 .001 排序 */
    suspend fun merge(
        parts: List<File>,
        target: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            require(parts.isNotEmpty()) { "没有可合并的分卷" }
            val sorted = parts.sortedBy { it.name.substringAfterLast('.').toIntOrNull() ?: Int.MAX_VALUE }
            val total = sorted.sumOf { it.length() }.coerceAtLeast(1L)
            var done = 0L
            target.outputStream().use { out ->
                val buf = ByteArray(256 * 1024)
                sorted.forEach { p ->
                    p.inputStream().use { input ->
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (done % (512 * 1024) == 0L) {
                                onProgress((done.toFloat() / total.toFloat()).coerceIn(0f, 1f))
                            }
                        }
                    }
                }
            }
            onProgress(1f)
            AppLogger.i("Split", "合并完成：${sorted.size} 卷 → ${target.name}")
            target
        }
    }

    /** 找出同一组的其他分卷（用于合并提示） */
    fun siblingsOf(part: File): List<File> {
        val m = PART_REGEX.matchEntire(part.name) ?: return listOf(part)
        val base = m.groupValues[1]
        return part.parentFile?.listFiles { f -> f.name.matches(Regex("""${Regex.escape(base)}\.\d{3}""")) }
            ?.sortedBy { it.name } ?: listOf(part)
    }
}
