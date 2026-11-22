package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveEngine
import com.ertyu.model.*
import kotlinx.coroutines.flow.update // 👈 补上这个关键导包
import kotlinx.coroutines.launch
import java.io.File

// ================== 解压逻辑 ==================
fun MainViewModel.openExtractOptions(items: List<FileItem>, isLeft: Boolean) {
    val defaultName = if (items.size == 1) {
        items[0].name.substringBeforeLast(".")
    } else {
        "批量解压_${System.currentTimeMillis()}"
    }
    ArchiveOptionsHolder.extractState.value = ExtractOptionsState(
        visible = true, targets = items, sourceIsLeft = isLeft, folderName = defaultName
    )
}

fun MainViewModel.cancelExtractOptions() {
    ArchiveOptionsHolder.extractState.value = ExtractOptionsState()
}

fun MainViewModel.executeAdvancedExtract(folderName: String, toOtherPanel: Boolean, deleteSource: Boolean) {
    val state = ArchiveOptionsHolder.extractState.value
    val targets = state.targets
    if (targets.isEmpty()) return
    cancelExtractOptions()

    val targetIsLeft = if (toOtherPanel) !state.sourceIsLeft else state.sourceIsLeft
    val parentDir = flowOf(targetIsLeft).value.currentUri?.path?.let(::File) ?: return

    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "正在解压", totalBytes = targets.size.toLong())
        var successCount = 0

        for ((index, item) in targets.withIndex()) {
            val archiveFile = item.uri.path?.let(::File) ?: continue
            _fileTransferState.update { it.copy(currentFile = archiveFile.name, copiedBytes = index.toLong()) }

            val finalDestDir = if (targets.size > 1) {
                File(parentDir, item.name.substringBeforeLast("."))
            } else {
                if (folderName.isNotBlank()) File(parentDir, folderName) else parentDir
            }

            val result = ArchiveEngine.extract(archiveFile, finalDestDir)
            if (result.isSuccess) {
                successCount++
                if (deleteSource) engine.delete(item.uri)
            }
        }
        
        _fileTransferState.value = FileTransferState()
        _toast.value = "解压完成，成功 $successCount 项"
        refresh(targetIsLeft)
    }
}

// ================== 压缩逻辑 ==================
fun MainViewModel.openCompressOptions(items: List<FileItem>, isLeft: Boolean) {
    val defaultExt = ".zip"
    val defaultName = if (items.size == 1) {
        val n = items[0].name
        if (n.contains(".")) n.substringBeforeLast(".") + defaultExt else n + defaultExt
    } else {
        "Archive_${System.currentTimeMillis()}$defaultExt"
    }
    ArchiveOptionsHolder.compressState.value = CompressOptionsState(
        visible = true, targets = items, sourceIsLeft = isLeft, fileName = defaultName
    )
}

fun MainViewModel.cancelCompressOptions() {
    ArchiveOptionsHolder.compressState.value = CompressOptionsState()
}

fun MainViewModel.executeAdvancedCompress(
    fileName: String,
    format: ArchiveFormat,
    level: Int,
    separate: Boolean,
    deleteSource: Boolean,
    toOtherPanel: Boolean,
    password: String? = null
) {
    val state = ArchiveOptionsHolder.compressState.value
    val targets = state.targets
    if (targets.isEmpty()) return
    cancelCompressOptions()

    val targetIsLeft = if (toOtherPanel) !state.sourceIsLeft else state.sourceIsLeft
    val parentDir = flowOf(targetIsLeft).value.currentUri?.path?.let(::File) ?: return
    
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "正在压缩", totalBytes = targets.size.toLong())
        
        try {
            // 手写一个后缀解析，避免依赖 ArchiveFormat.extensions
            val ext = when(format) {
                ArchiveFormat.ZIP -> ".zip"
                ArchiveFormat.TAR -> ".tar"
                ArchiveFormat.TAR_GZ -> ".tar.gz"
                ArchiveFormat.TAR_BZ2 -> ".tar.bz2"
                ArchiveFormat.TAR_XZ -> ".tar.xz"
                ArchiveFormat.TAR_ZST -> ".tar.zst"
                ArchiveFormat.SEVEN_Z -> ".7z"
                else -> ".zip"
            }

            var okCount = 0
            var failCount = 0
            val successSources = mutableListOf<File>()

            if (separate && targets.size > 1) {
                // 多文件分别压缩
                for ((index, item) in targets.withIndex()) {
                    val src = File(item.uri.path ?: continue)
                    val dest = File(parentDir, "${src.nameWithoutExtension}$ext")
                    _fileTransferState.update { it.copy(currentFile = src.name, copiedBytes = index.toLong()) }
                    val result = ArchiveEngine.compress(
                        sources = listOf(src),
                        targetFile = dest,
                        format = format,
                        password = password,
                        compressionLevel = level
                    )
                    if (result.isSuccess) {
                        okCount++
                        successSources.add(src)
                    } else {
                        failCount++
                        AppLogger.e("Compress", "压缩失败：${src.absolutePath}", result.exceptionOrNull())
                    }
                }
            } else {
                // 合并压缩
                val sources = targets.mapNotNull { it.uri.path?.let(::File) }
                val dest = File(parentDir, fileName)
                val result = ArchiveEngine.compress(
                    sources = sources,
                    targetFile = dest,
                    format = format,
                    password = password,
                    compressionLevel = level
                )
                if (result.isSuccess) {
                    okCount = 1
                    successSources.addAll(sources)
                } else {
                    failCount = 1
                    AppLogger.e("Compress", "压缩失败：${dest.absolutePath}", result.exceptionOrNull())
                }
            }

            _toast.value = when {
                failCount == 0 -> "压缩完成"
                okCount == 0 -> "压缩失败：${if (password != null) "密码或存储空间可能有问题" else "请检查存储空间或文件名"}"
                else -> "压缩完成：成功 $okCount 个，失败 $failCount 个"
            }

            if (deleteSource && successSources.isNotEmpty()) {
                // 只删除压缩成功的源文件，失败的保留
                successSources.forEach { f ->
                    targets.firstOrNull { it.path == f.absolutePath }?.let { engine.delete(it.uri) }
                }
                _toast.value = (_toast.value ?: "") + "（已删除 ${successSources.size} 个源文件）"
            }
            refresh(targetIsLeft)
            if (state.sourceIsLeft != targetIsLeft) refresh(state.sourceIsLeft)
        } catch (e: Exception) {
            _toast.value = "压缩失败: ${e.message}"
        } finally {
            _fileTransferState.value = FileTransferState()
        }
    }
}