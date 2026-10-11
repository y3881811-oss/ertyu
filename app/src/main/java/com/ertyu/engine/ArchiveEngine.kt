package com.ertyu.engine

import com.ertyu.model.ArchiveFormat
import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream

/**
 * 统一处理 zip / tar / tar.gz / tar.bz2 / tar.xz / tar.zst / 7z / rar
 * 等压缩格式的解压（zip 走 ZipEngine，更快）。
 */
object ArchiveEngine {

    private const val BUF_SIZE = 16 * 1024

    suspend fun extract(
        archiveFile: File,
        targetDir: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val format = ArchiveFormat.detect(archiveFile.name)
                ?: throw IllegalArgumentException("不支持的格式: ${archiveFile.name}")
            if (!targetDir.exists()) targetDir.mkdirs()
            val canonicalTarget = targetDir.canonicalPath

            when (format) {
                ArchiveFormat.ZIP ->
                    ZipEngine.extract(archiveFile, targetDir, onProgress).getOrThrow()

                ArchiveFormat.TAR ->
                    openTar(archiveFile, null).use {
                        extractTar(it, targetDir, canonicalTarget, onProgress)
                    }

                ArchiveFormat.TAR_GZ ->
                    openTar(archiveFile, "gz").use {
                        extractTar(it, targetDir, canonicalTarget, onProgress)
                    }

                ArchiveFormat.TAR_BZ2 ->
                    openTar(archiveFile, "bz2").use {
                        extractTar(it, targetDir, canonicalTarget, onProgress)
                    }

                ArchiveFormat.TAR_XZ ->
                    openTar(archiveFile, "xz").use {
                        extractTar(it, targetDir, canonicalTarget, onProgress)
                    }

                ArchiveFormat.TAR_ZST ->
                    openTar(archiveFile, "zst").use {
                        extractTar(it, targetDir, canonicalTarget, onProgress)
                    }

                ArchiveFormat.GZ -> {
                    val outName = archiveFile.name.removeSuffix(".gz")
                    GzipCompressorInputStream(
                        BufferedInputStream(archiveFile.inputStream())
                    ).use { writeSingleStream(it, File(targetDir, outName)) }
                }

                ArchiveFormat.BZ2 -> {
                    val outName = archiveFile.name.removeSuffix(".bz2")
                    BZip2CompressorInputStream(
                        BufferedInputStream(archiveFile.inputStream())
                    ).use { writeSingleStream(it, File(targetDir, outName)) }
                }

                ArchiveFormat.XZ -> {
                    val outName = archiveFile.name.removeSuffix(".xz")
                    XZCompressorInputStream(
                        BufferedInputStream(archiveFile.inputStream())
                    ).use { writeSingleStream(it, File(targetDir, outName)) }
                }

                ArchiveFormat.ZST -> {
                    val outName = archiveFile.name.removeSuffix(".zst")
                    ZstdCompressorInputStream(
                        BufferedInputStream(archiveFile.inputStream())
                    ).use { writeSingleStream(it, File(targetDir, outName)) }
                }

                ArchiveFormat.SEVEN_Z ->
                    extract7z(archiveFile, targetDir, canonicalTarget, onProgress)

                ArchiveFormat.RAR ->
                    extractRar(archiveFile, targetDir, canonicalTarget)
            }
        }
    }

    // ==================== tar ====================

    private fun openTar(archiveFile: File, compressor: String?): TarArchiveInputStream {
        val base: InputStream = BufferedInputStream(archiveFile.inputStream())
        val decompressed: InputStream = when (compressor) {
            null -> base
            "gz" -> GzipCompressorInputStream(base)
            "bz2" -> BZip2CompressorInputStream(base)
            "xz" -> XZCompressorInputStream(base)
            "zst" -> ZstdCompressorInputStream(base)
            else -> throw IllegalArgumentException("未知压缩器: $compressor")
        }
        return TarArchiveInputStream(decompressed)
    }

    private fun extractTar(
        tar: TarArchiveInputStream,
        targetDir: File,
        canonicalTarget: String,
        onProgress: (Int, Int) -> Unit
    ) {
        onProgress(0, -1)
        var current = 0
        var entry = tar.nextEntry
        while (entry != null) {
            val outFile = File(targetDir, entry.name)
            if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                throw SecurityException("非法路径: ${entry.name}")
            }
            if (entry.isDirectory) {
                outFile.mkdirs()
            } else {
                outFile.parentFile?.mkdirs()
                BufferedOutputStream(outFile.outputStream()).use { out ->
                    tar.copyTo(out, BUF_SIZE)
                }
            }
            current++
            onProgress(current, -1)
            entry = tar.nextEntry
        }
    }

    // ==================== 单文件压缩流（.gz / .bz2 / .xz / .zst） ====================

    private fun writeSingleStream(input: InputStream, outFile: File) {
        outFile.parentFile?.mkdirs()
        BufferedOutputStream(outFile.outputStream()).use { out ->
            input.copyTo(out, BUF_SIZE)
        }
    }

    // ==================== 7z ====================

    private fun extract7z(
        archiveFile: File,
        targetDir: File,
        canonicalTarget: String,
        onProgress: (Int, Int) -> Unit
    ) {
        SevenZFile.builder().setFile(archiveFile).get().use { sz ->
            onProgress(0, -1)
            var current = 0
            val buf = ByteArray(BUF_SIZE)
            var entry = sz.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (!outFile.canonicalPath.startsWith(canonicalTarget)) {
                    throw SecurityException("非法路径: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(outFile.outputStream()).use { out ->
                        while (true) {
                            val n = sz.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                        }
                    }
                }
                current++
                onProgress(current, -1)
                entry = sz.nextEntry
            }
        }
    }

    // ==================== rar ====================

    private fun extractRar(
        archiveFile: File,
        targetDir: File,
        canonicalTarget: String
    ) {
        // junrar 的 Junrar.extract 会自行遍历，这里只做目标目录检查
        if (!targetDir.canonicalPath.startsWith(canonicalTarget)) {
            throw SecurityException("非法目标路径")
        }
        Junrar.extract(archiveFile, targetDir)
    }
}