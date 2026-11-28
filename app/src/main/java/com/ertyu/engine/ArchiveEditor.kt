package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * 压缩包内编辑（目前只支持 ZIP）：
 * 在临时文件里重写整个包，校验通过后再原子替换原包，失败不破坏原文件。
 */
object ArchiveEditor {

    private const val BUF = 64 * 1024

    fun supports(file: File): Boolean = file.name.endsWith(".zip", ignoreCase = true)

    /**
     * @param remove 要删除的条目完整路径
     * @param rename 旧路径 -> 新路径（只改末段名字，调用方负责拼好）
     * @param add    本地文件 -> 包内路径
     * @return 成功时返回新包的条目数
     */
    suspend fun rewrite(
        archive: File,
        remove: Set<String> = emptySet(),
        rename: Map<String, String> = emptyMap(),
        add: List<Pair<File, String>> = emptyList(),
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!supports(archive)) return@withContext Result.failure(IllegalArgumentException("目前只支持 ZIP 包内编辑"))
        if (!archive.isFile) return@withContext Result.failure(IllegalArgumentException("压缩包不存在"))

        val tmp = File(archive.parentFile, ".${archive.name}.edit_tmp")
        if (tmp.exists()) tmp.delete()

        try {
            var written = 0
            ZipFile(archive).use { zip ->
                ZipOutputStream(BufferedOutputStream(tmp.outputStream())).use { out ->
                    val all = zip.entries().toList()
                    all.forEachIndexed { index, e ->
                        currentCoroutineContext().ensureActive()
                        if (e.name in remove) return@forEachIndexed
                        val targetName = rename[e.name] ?: e.name
                        val isDir = e.isDirectory
                        val entry = ZipEntry(targetName).apply {
                            if (isDir) {
                                // 目录条目
                            } else {
                                time = e.time
                            }
                        }
                        out.putNextEntry(entry)
                        if (!isDir) {
                            zip.getInputStream(e).use { it.copyTo(out, BUF) }
                        }
                        out.closeEntry()
                        written++
                        onProgress(index + 1, all.size + add.size)
                    }
                    add.forEach { (file, path) ->
                        currentCoroutineContext().ensureActive()
                        if (!file.isFile) return@forEach
                        out.putNextEntry(ZipEntry(path))
                        BufferedInputStream(file.inputStream()).use { it.copyTo(out, BUF) }
                        out.closeEntry()
                        written++
                    }
                }
            }

            // 校验：新包能正常打开
            val check = ZipFile(tmp).use { it.size() }
            if (check <= 0 && written > 0) {
                tmp.delete()
                return@withContext Result.failure(IllegalStateException("新包校验失败，已保留原压缩包"))
            }

            try {
                Files.move(
                    tmp.toPath(), archive.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                )
            } catch (e: Exception) {
                // 跨文件系统等场景：退化为复制
                tmp.copyTo(archive, overwrite = true)
                tmp.delete()
            }
            Result.success(check)
        } catch (e: Exception) {
            try {
                if (tmp.exists()) tmp.delete()
            } catch (_: Exception) {
            }
            Result.failure(e)
        }
    }
}
