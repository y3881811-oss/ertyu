package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ArchiveBrowser
import com.ertyu.model.ArchiveEntry
import com.ertyu.model.ArchiveViewerState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openArchiveViewer(isLeft: Boolean, item: FileItem) {
    val path = item.uri.path ?: run {
        _toast.value = "无效路径"
        return
    }
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
            ArchiveViewerStateHolder.state.update {
                it.copy(isLoading = false, entries = entries)
            }
        }.onFailure { e ->
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

/** 进入子目录 */
fun MainViewModel.enterArchiveDir(dirPath: String) {
    ArchiveViewerStateHolder.state.update { it.copy(currentDir = dirPath, filter = "") }
}

/** 返回上级目录 */
fun MainViewModel.goUpArchiveDir() {
    ArchiveViewerStateHolder.state.update { state ->
        if (state.currentDir.isEmpty()) return@update state
        val parentPath = state.currentDir.trimEnd('/').substringBeforeLast('/', "")
        val newPath = if (parentPath.isEmpty()) "" else "$parentPath/"
        state.copy(currentDir = newPath, filter = "")
    }
}

/** 提取单个条目到源面板当前目录 */
fun MainViewModel.extractArchiveEntry(entry: ArchiveEntry) {
    if (entry.isDirectory) return
    val s = ArchiveViewerStateHolder.state.value
    val targetDir = flowOf(s.sourceIsLeft)
        .value.currentUri?.path?.let(::File) ?: run {
        _toast.value = "无目标目录"
        return
    }
    viewModelScope.launch {
        val result = ArchiveBrowser.extractEntry(
            archiveFile = File(s.archivePath),
            entryPath = entry.path,
            entryName = entry.name,
            targetDir = targetDir
        )
        result.onSuccess { out ->
            _toast.value = "已提取：${out.name}"
            refresh(s.sourceIsLeft)
        }.onFailure { e ->
            _toast.value = "提取失败：${e.message}"
        }
    }
}

/**
 * 从压缩包浏览界面一键全部解压 (带高级参数)
 */
fun MainViewModel.extractAllFromViewer(
    targetDirName: String,
    toOtherPanel: Boolean,
    deleteSource: Boolean
) {
    val state = ArchiveViewerStateHolder.state.value
    if (state.archivePath.isEmpty()) return
    
    val fileUri = android.net.Uri.fromFile(java.io.File(state.archivePath))
    val item = FileItem(
        name = state.archiveName,
        uriString = fileUri.toString(),
        uri = fileUri,
        isDirectory = false,
        path = state.archivePath
    )

    closeArchiveViewer()
    
    // 👇 调用统一的解压逻辑
    extractArchive(
        isLeft = state.sourceIsLeft,
        item = item,
        targetDirName = targetDirName,
        toOtherPanel = toOtherPanel,
        deleteSource = deleteSource
    )
}