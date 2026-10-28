package com.ertyu.engine

import com.ertyu.model.ArchiveEntry
import com.ertyu.model.ArchiveFormat
import com.github.junrar.Archive
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
import java.util.zip.ZipFile

object ArchiveBrowser {

    // ==================== 列出内部条目 ====================

    suspend fun list(archiveFile: File): Result<List<ArchiveEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (!archiveFile.exists()) {
                    throw IllegalArgumentException("文件不存在")
                }
                val format = ArchiveFormat.detect(archiveFile.name)
                    ?: throw IllegalArgumentException("不支持的格式")
                when (format) {
                    ArchiveFormat.ZIP -> listZip(archiveFile)
                    ArchiveFormat.TAR -> listTar(archiveFile, null)
                    ArchiveFormat.TAR_GZ -> listTar(archiveFile, "gz")
                    ArchiveFormat.TAR_BZ2 -> listTar(archiveFile, "bz2")
                    ArchiveFormat.TAR_XZ -> listTar(archiveFile, "xz")
                    ArchiveFormat.TAR_ZST -> listTar(archiveFile, "zst")
                    ArchiveFormat.GZ -> listSingle(archiveFile, ".gz")
                    ArchiveFormat.BZ2 -> listSingle(archiveFile, ".bz2")
                    ArchiveFormat.XZ -> listSingle(archiveFile, ".xz")
                    ArchiveFormat.ZST -> listSingle(archiveFile, ".zst")
                    ArchiveFormat.SEVEN_Z -> list7z(archiveFile)
                    ArchiveFormat.RAR -> listRar(archiveFile)
                }
            }
        }

    private fun listZip(file: File): List<ArchiveEntry> {
        val result = ArrayList<ArchiveEntry>()
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                val path = e.name
                val isDir = e.isDirectory
                val name = if (isDir)
                    path.trimEnd('/').substringAfterLast('/')
                else path.substringAfterLast('/')
                val size = if (isDir) 0L else e.size.coerceAtLeast(0)
                val comp = if (isDir) 0L else e.compressedSize.coerceAtLeast(0)
                result.add(
                    ArchiveEntry(
                        path = path,
                        name = name,
                        isDirectory = isDir,
                        size = size,
                        compressedSize = comp,
                        depth = path.trimEnd('/').count { it == '/' }
                    )
                )
            }
        }
        result.sortWith(compareBy({ it.path.lowercase() }))
        return result
    }

    private fun listTar(file: File, compressor: String?): List<ArchiveEntry> {
        val result = ArrayList<ArchiveEntry>()
        openTar(file, compressor).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val path = entry.name
                val isDir = entry.isDirectory
                val name = if (isDir)
                    path.trimEnd('/').substringAfterLast('/')
                else path.substringAfterLast('/')
                val size = entry.size.coerceAtLeast(0)
                result.add(
                    ArchiveEntry(
                        path = path,
                        name = name,
                        isDirectory = isDir,
                        size = size,
                        compressedSize = 0,
                        depth = path.trimEnd('/').count { it == '/' }
                    )
                )
                entry = tar.nextEntry
            }
        }
        result.sortWith(compareBy({ it.path.lowercase() }))
        return result
    }

    private fun openTar(file: File, compressor: String?): TarArchiveInputStream {
        val base: InputStream = BufferedInputStream(file.inputStream())
        val decompressed: InputStream = when (compressor) {
            null -> base
            "gz" -> GzipCompressorInputStream(base)
            "bz2" -> BZip2CompressorInputStream(base)
            "xz" -> XZCompressorInputStream(base)
            "zst" -> ZstdCompressorInputStream(base)
            else -> throw IllegalArgumentException("未知压缩器")
        }
        return TarArchiveInputStream(decompressed)
    }

    private fun listSingle(file: File, suffix: String): List<ArchiveEntry> {
        val name = file.name.removeSuffix(suffix)
        return listOf(
            ArchiveEntry(
                path = name,
                name = name,
                isDirectory = false,
                size = 0L,
                compressedSize = file.length(),
                depth = 0
            )
        )
    }

    private fun list7z(file: File): List<ArchiveEntry> {
        val result = ArrayList<ArchiveEntry>()
        SevenZFile.builder().setFile(file).get().use { sz ->
            var entry = sz.nextEntry
            while (entry != null) {
                val path = entry.name
                val isDir = entry.isDirectory
                val name = if (isDir)
                    path.trimEnd('/').substringAfterLast('/')
                else path.substringAfterLast('/')
                val size = if (isDir) 0L else entry.size.coerceAtLeast(0)
                result.add(
                    ArchiveEntry(
                        path = path,
                        name = name,
                        isDirectory = isDir,
                        size = size,
                        compressedSize = 0,
                        depth = path.trimEnd('/').count { it == '/' }
                    )
                )
                entry = sz.nextEntry
            }
        }
        result.sortWith(compareBy({ it.path.lowercase() }))
        return result
    }

    private fun listRar(file: File): List<ArchiveEntry> {
        val result = ArrayList<ArchiveEntry>()
        val archive = Archive(file)
        try {
            var fh = archive.nextFileHeader()
            while (fh != null) {
                val path = fh.fileNameString.replace('\\', '/')
                val isDir = fh.isDirectory
                val name = if (isDir)
                    path.trimEnd('/').substringAfterLast('/')
                else path.substringAfterLast('/')
                val size = fh.fullUnpackSize
                val comp = fh.packSize
                result.add(
                    ArchiveEntry(
                        path = path,
                        name = name,
                        isDirectory = isDir,
                        size = size,
                        compressedSize = comp,
                        depth = path.trimEnd('/').count { it == '/' }
                    )
                )
                fh = archive.nextFileHeader()
            }
        } finally {
            archive.close()
        }
        result.sortWith(compareBy({ it.path.lowercase() }))
        return result
    }

    // ==================== 提取单个条目 ====================

    suspend fun extractEntry(
        archiveFile: File,
        entryPath: String,
        entryName: String,
        targetDir: File
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!targetDir.exists()) targetDir.mkdirs()
            val format = ArchiveFormat.detect(archiveFile.name)
                ?: throw IllegalArgumentException("不支持的格式")
            val outFile = File(targetDir, entryName)

            when (format) {
                ArchiveFormat.ZIP ->
                    extractZipEntry(archiveFile, entryPath, outFile)
                ArchiveFormat.TAR ->
                    extractTarEntry(archiveFile, null, entryPath, outFile)
                ArchiveFormat.TAR_GZ ->
                    extractTarEntry(archiveFile, "gz", entryPath, outFile)
                ArchiveFormat.TAR_BZ2 ->
                    extractTarEntry(archiveFile, "bz2", entryPath, outFile)
                ArchiveFormat.TAR_XZ ->
                    extractTarEntry(archiveFile, "xz", entryPath, outFile)
                ArchiveFormat.TAR_ZST ->
                    extractTarEntry(archiveFile, "zst", entryPath, outFile)
                ArchiveFormat.GZ ->
                    extractSingle(archiveFile, "gz", outFile)
                ArchiveFormat.BZ2 ->
                    extractSingle(archiveFile, "bz2", outFile)
                ArchiveFormat.XZ ->
                    extractSingle(archiveFile, "xz", outFile)
                ArchiveFormat.ZST ->
                    extractSingle(archiveFile, "zst", outFile)
                ArchiveFormat.SEVEN_Z ->
                    extract7zEntry(archiveFile, entryPath, outFile)
                ArchiveFormat.RAR ->
                    extractRarEntry(archiveFile, entryPath, outFile)
            }
            outFile
        }
    }

    private fun extractZipEntry(
        archiveFile: File,
        entryPath: String,
        outFile: File
    ) {
        ZipFile(archiveFile).use { zip ->
            val e = zip.getEntry(entryPath)
                ?: throw IllegalArgumentException("找不到：$entryPath")
            outFile.parentFile?.mkdirs()
            zip.getInputStream(e).use { input ->
                BufferedOutputStream(outFile.outputStream()).use { out ->
                    input.copyTo(out, 64 * 1024)
                }
            }
        }
    }

    private fun extractTarEntry(
        archiveFile: File,
        compressor: String?,
        entryPath: String,
        outFile: File
    ) {
        openTar(archiveFile, compressor).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                if (entry.name == entryPath) {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(outFile.outputStream()).use { out ->
                        tar.copyTo(out, 64 * 1024)
                    }
                    return
                }
                entry = tar.nextEntry
            }
        }
        throw IllegalArgumentException("找不到：$entryPath")
    }

    private fun extractSingle(
        archiveFile: File,
        compressor: String,
        outFile: File
    ) {
        val base: InputStream = BufferedInputStream(archiveFile.inputStream())
        val input: InputStream = when (compressor) {
            "gz" -> GzipCompressorInputStream(base)
            "bz2" -> BZip2CompressorInputStream(base)
            "xz" -> XZCompressorInputStream(base)
            "zst" -> ZstdCompressorInputStream(base)
            else -> throw IllegalArgumentException("未知压缩器")
        }
        input.use { ins ->
            outFile.parentFile?.mkdirs()
            BufferedOutputStream(outFile.outputStream()).use { out ->
                ins.copyTo(out, 64 * 1024)
            }
        }
    }

    private fun extract7zEntry(
        archiveFile: File,
        entryPath: String,
        outFile: File
    ) {
        SevenZFile.builder().setFile(archiveFile).get().use { sz ->
            var entry = sz.nextEntry
            val buf = ByteArray(64 * 1024)
            while (entry != null) {
                if (entry.name == entryPath) {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(outFile.outputStream()).use { out ->
                        while (true) {
                            val n = sz.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                        }
                    }
                    return
                }
                entry = sz.nextEntry
            }
        }
        throw IllegalArgumentException("找不到：$entryPath")
    }

    private fun extractRarEntry(
        archiveFile: File,
        entryPath: String,
        outFile: File
    ) {
        val archive = Archive(archiveFile)
        try {
            var fh = archive.nextFileHeader()
            while (fh != null) {
                val path = fh.fileNameString.replace('\\', '/')
                if (path == entryPath) {
                    outFile.parentFile?.mkdirs()
                    BufferedOutputStream(outFile.outputStream()).use { out ->
                        archive.extractFile(fh, out)
                    }
                    return
                }
                fh = archive.nextFileHeader()
            }
        } finally {
            archive.close()
        }
        throw IllegalArgumentException("找不到：$entryPath")
    }
}