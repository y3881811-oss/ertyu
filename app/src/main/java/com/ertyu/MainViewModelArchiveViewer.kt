package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveBrowser
import com.ertyu.engine.ArchiveEngine
import com.ertyu.model.ArchiveEntry
import com.ertyu.model.ArchiveViewerState
import com.ertyu.model.FileItem
import com.ertyu.model.FileTransferState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openArchiveViewer(isLeft: Boolean, item: FileItem) {
    val path = item.uri.path ?: run {
        _toast.value = "无效路径"
        return
    }
    AppLogger.i("Archive", "正在打开压缩包: $path")
    ArchiveViewerStateHolder.state.value = ArchiveViewerState(
        visible = true,
        archivePath = path,
        archiveName = item.name,
        sourceIsLeft = isLeft,
        currentDir = "",
        isLoading = true
    )
    viewModelScope.launch {
        val result = ArchiveBrowser.list(File(path))
        result.onSuccess { entries ->
            AppLogger.i("Archive", "读取压缩包成功，共 ${entries.size} 个条目")
            ArchiveViewerStateHolder.state.update {
                it.copy(isLoading = false, entries = entries)
            }
        }.onFailure { e ->
            AppLogger.e("Archive", "读取压缩包失败: ${item.name}", e)
            ArchiveViewerStateHolder.state.update {
                it.copy(isLoading = false, error = e.message ?: "读取失败")
            }
        }
    }
}

fun MainViewModel.closeArchiveViewer() {
    ArchiveViewerStateHolder.state.value = ArchiveViewerState()
}

fun MainViewModel.updateArchiveViewerFilter(q: String) {
    ArchiveViewerStateHolder.state.update { it.copy(filter = q) }
}

fun MainViewModel.enterArchiveDir(dirPath: String) {
    ArchiveViewerStateHolder.state.update { it.copy(currentDir = dirPath, filter = "") }
}

fun MainViewModel.goUpArchiveDir() {
    ArchiveViewerStateHolder.state.update { state ->
        if (state.currentDir.isEmpty()) return@update state
        val parentPath = state.currentDir.trimEnd('/').substringBeforeLast('/', "")
        val newPath = if (parentPath.isEmpty()) "" else "$parentPath/"
        state.copy(currentDir = newPath, filter = "")
    }
}

fun MainViewModel.extractArchiveEntry(entry: ArchiveEntry) {
    if (entry.isDirectory) return
    val s = ArchiveViewerStateHolder.state.value
    val targetDir = flowOf(s.sourceIsLeft).value.currentUri?.path?.let(::File) ?: run {
        _toast.value = "无目标目录"
        return
    }
    AppLogger.i("Archive", "提取单文件: ${entry.name} -> ${targetDir.absolutePath}")
    
    viewModelScope.launch {
        val result = ArchiveBrowser.extractEntry(
            archiveFile = File(s.archivePath),
            entryPath = entry.path,
            entryName = entry.name,
            targetDir = targetDir
        )
        result.onSuccess { out ->
            AppLogger.i("Archive", "提取成功: ${out.name}")
            _toast.value = "已提取：${out.name}"
            refresh(s.sourceIsLeft)
        }.onFailure { e ->
            AppLogger.e("Archive", "提取失败: ${entry.name}", e)
            _toast.value = "提取失败：${e.message}"
        }
    }
}

fun MainViewModel.extractAllFromViewer(
    targetDirName: String = "",
    toOtherPanel: Boolean = false,
    deleteSource: Boolean = false
) {
    val state = ArchiveViewerStateHolder.state.value
    if (state.archivePath.isEmpty()) return
    val archiveFile = File(state.archivePath)
    val targetIsLeft = if (toOtherPanel) !state.sourceIsLeft else state.sourceIsLeft
    
    val targetParentDir = flowOf(targetIsLeft).value.currentUri?.path?.let(::File) ?: return
    val finalTargetDir = if (targetDirName.isNotBlank()) File(targetParentDir, targetDirName) else targetParentDir

    AppLogger.i("Archive", "开始解压全部: ${archiveFile.name} -> ${finalTargetDir.absolutePath}, 删源文件: $deleteSource")

    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "正在解压", currentFile = archiveFile.name)
        val result = ArchiveEngine.extract(
            archiveFile = archiveFile,
            targetDir = finalTargetDir,
            onProgress = { current, total ->
                _fileTransferState.update { it.copy(copiedBytes = current.toLong(), totalBytes = total.toLong()) }
            }
        )

        result.onSuccess {
            AppLogger.i("Archive", "解压成功: ${archiveFile.name}")
            _toast.value = "已成功解压至: ${finalTargetDir.absolutePath}"
            if (deleteSource) {
                engine.delete(android.net.Uri.fromFile(archiveFile))
                AppLogger.i("Archive", "已删除解压源文件: ${archiveFile.name}")
            }
            refresh(targetIsLeft)
            closeArchiveViewer()
        }.onFailure { e ->
            AppLogger.e("Archive", "解压失败: ${archiveFile.name}", e)
            _toast.value = "解压失败: ${e.message}"
        }
        _fileTransferState.value = FileTransferState()
    }
}