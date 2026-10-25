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

/** 新增：从压缩包浏览界面一键全部解压 */
fun MainViewModel.extractAllFromViewer() {
    val state = ArchiveViewerStateHolder.state.value
    val item = FileItem(
        name = state.archiveName,
        uri = android.net.Uri.fromFile(java.io.File(state.archivePath)),
        isDirectory = false,
        path = state.archivePath
    )
    closeArchiveViewer()
    // 调用解压到当前目录的方法
    extractZipHere(state.sourceIsLeft, item)
}