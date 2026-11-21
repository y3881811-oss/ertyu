package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveEngine
import com.ertyu.model.*
import kotlinx.coroutines.launch
import java.io.File

// ================== 解压逻辑 ==================
fun MainViewModel.openExtractOptions(items: List<FileItem>, isLeft: Boolean) {
    // 如果是多个文件，默认解压到各自同名文件夹
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

            // 确定每个文件的目标路径
            val finalDestDir = if (targets.size > 1) {
                // 多文件时，每个包解压到以自己名字命名的文件夹里
                File(parentDir, item.name.substringBeforeLast("."))
            } else {
                // 单文件，看用户输入
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
    fileName: String, format: ArchiveFormat, level: Int, separate: Boolean, deleteSource: Boolean, toOtherPanel: Boolean
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
            if (separate && targets.size > 1) {
                // 多文件分别压缩
                for ((index, item) in targets.withIndex()) {
                    val src = File(item.uri.path ?: continue)
                    val dest = File(parentDir, "${src.nameWithoutExtension}${format.extensions.firstOrNull()?.let { ".$it" } ?: ".zip"}")
                    _fileTransferState.update { it.copy(currentFile = src.name, copiedBytes = index.toLong()) }
                    ArchiveEngine.compress(listOf(src), dest, format)
                }
            } else {
                // 合并压缩
                val sources = targets.mapNotNull { it.uri.path?.let(::File) }
                val dest = File(parentDir, fileName)
                ArchiveEngine.compress(sources, dest, format)
            }

            _toast.value = "压缩完成"
            if (deleteSource) {
                targets.forEach { engine.delete(it.uri) }
                _toast.value = "压缩完成并删除了源文件"
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