package com.ertyu.engine

import com.ertyu.model.ArchiveFormat
import com.github.junrar.Junrar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.zstandard.ZstdCompressorInputStream
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ArchiveEngine {

    private const val BUF_SIZE = 16 * 1024

    // ==================== 解压 ====================

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

    // ==================== 压缩 ====================

    suspend fun compress(
        sources: List<File>,
        targetFile: File,
        format: ArchiveFormat,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val entries = mutableListOf<Pair<File, String>>()
            sources.forEach { src -> collect(src, src.name, entries) }
            if (entries.isEmpty()) throw IllegalArgumentException("没有可压缩的内容")

            val total = entries.size
            onProgress(0, total)

            when (format) {
                ArchiveFormat.ZIP ->
                    compressZip(entries, targetFile, onProgress)
                ArchiveFormat.TAR ->
                    compressTar(entries, targetFile, null, onProgress)
                ArchiveFormat.TAR_GZ ->
                    compressTar(entries, targetFile, "gz", onProgress)
                ArchiveFormat.SEVEN_Z ->
                    compress7z(entries, targetFile, onProgress)
                else ->
                    throw IllegalArgumentException("不支持压缩为 ${format.label}")
            }
        }
    }

    private fun compress7z(
        entries: List<Pair<File, String>>,
        target: File,
        onProgress: (Int, Int) -> Unit
    ) {
        SevenZOutputFile(target).use { out ->
            var current = 0
            for ((file, name) in entries) {
                val entryName = if (file.isDirectory) "$name/" else name
                val entry = SevenZArchiveEntry().apply {
                    this.name = entryName
                    this.isDirectory = file.isDirectory
                    if (file.isFile) {
                        this.size = file.length()
                    }
                }
                out.putArchiveEntry(entry)
                if (file.isFile) {
                    BufferedInputStream(file.inputStream()).use { input ->
                        input.copyTo(out, BUF_SIZE)
                    }
                }
                out.closeArchiveEntry()
                current++
                onProgress(current, entries.size)
            }
        }
    }

    // ==================== 其他辅助方法 ====================

    private fun compressZip(
        entries: List<Pair<File, String>>,
        target: File,
        onProgress: (Int, Int) -> Unit
    ) {
        ZipOutputStream(BufferedOutputStream(target.outputStream())).use { zos ->
            zos.setLevel(6)
            var current = 0
            for ((file, name) in entries) {
                val entryName = if (file.isDirectory) "$name/" else name
                val entry = ZipEntry(entryName)
                zos.putNextEntry(entry)
                if (file.isFile) {
                    BufferedInputStream(file.inputStream()).use { input ->
                        input.copyTo(zos, BUF_SIZE)
                    }
                }
                zos.closeEntry()
                current++
                onProgress(current, entries.size)
            }
        }
    }

    private fun compressTar(
        entries: List<Pair<File, String>>,
        target: File,
        compressor: String?,
        onProgress: (Int, Int) -> Unit
    ) {
        val base: OutputStream = BufferedOutputStream(target.outputStream())
        val out: OutputStream = when (compressor) {
            null -> base
            "gz" -> GzipCompressorOutputStream(base)
            else -> throw IllegalArgumentException("未知压缩器: $compressor")
        }
        TarArchiveOutputStream(out).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            tar.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX)
            var current = 0
            for ((file, name) in entries) {
                val entryName = if (file.isDirectory) "$name/" else name
                val entry = TarArchiveEntry(file, entryName)
                tar.putArchiveEntry(entry)
                if (file.isFile) {
                    BufferedInputStream(file.inputStream()).use { input ->
                        input.copyTo(tar, BUF_SIZE)
                    }
                }
                tar.closeArchiveEntry()
                current++
                onProgress(current, entries.size)
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

    private fun writeSingleStream(input: InputStream, outFile: File) {
        outFile.parentFile?.mkdirs()
        BufferedOutputStream(outFile.outputStream()).use { out ->
            input.copyTo(out, BUF_SIZE)
        }
    }

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

    private fun extractRar(
        archiveFile: File,
        targetDir: File,
        canonicalTarget: String
    ) {
        if (!targetDir.canonicalPath.startsWith(canonicalTarget)) {
            throw SecurityException("非法目标路径")
        }
        Junrar.extract(archiveFile, targetDir)
    }
}