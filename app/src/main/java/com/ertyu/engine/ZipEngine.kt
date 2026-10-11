package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipEngine {

    /** 解压 zipFile 到 targetDir，onProgress(current, total) 在 IO 线程回调 */
    suspend fun extract(
        zipFile: File,
        targetDir: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!targetDir.exists()) targetDir.mkdirs()

            // 第一遍：数条目总数
            var total = 0
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                while (zis.nextEntry != null) total++
            }
            onProgress(0, total)

            val canonicalTarget = targetDir.canonicalPath
            var current = 0

            // 第二遍：真正解压
            ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(targetDir, entry.name)
                    // 防 zip slip
                    if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                        throw SecurityException("非法路径: ${entry.name}")
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        BufferedOutputStream(outFile.outputStream()).use { out ->
                            zis.copyTo(out, 8 * 1024)
                        }
                    }
                    current++
                    onProgress(current, total)
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    /** 将 sources（文件或目录）压缩到 targetZip */
    suspend fun compress(
        sources: List<File>,
        targetZip: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // 收集所有要写入的条目
            val entries = mutableListOf<Pair<File, String>>()
            sources.forEach { src ->
                collect(src, src.name, entries)
            }
            val total = entries.size
            onProgress(0, total)

            var current = 0
            ZipOutputStream(BufferedOutputStream(targetZip.outputStream())).use { zos ->
                entries.forEach { (file, entryName) ->
                    val entry = ZipEntry(entryName + if (file.isDirectory) "/" else "")
                    zos.putNextEntry(entry)
                    if (file.isFile) {
                        BufferedInputStream(file.inputStream()).use { input ->
                            input.copyTo(zos, 8 * 1024)
                        }
                    }
                    zos.closeEntry()
                    current++
                    onProgress(current, total)
                }
            }
        }
    }

    private fun collect(file: File, entryName: String, out: MutableList<Pair<File, String>>) {
        if (file.isDirectory) {
            out.add(file to entryName)
            file.listFiles()?.forEach { child ->
                collect(child, "$entryName/${child.name}", out)
            }
        } else {
            out.add(file to entryName)
        }
    }
}