package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveBrowser
import com.ertyu.engine.ArchiveEditor
import com.ertyu.model.ArchiveEntry
import com.ertyu.model.ArchiveViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun MainViewModel.reloadArchiveViewer(archivePath: String) {
    viewModelScope.launch {
        val result = ArchiveBrowser.list(File(archivePath))
        result.onSuccess { list ->
            ArchiveViewerStateHolder.state.update {
                it.copy(entries = list, isLoading = false, error = null)
            }
        }.onFailure { e ->
            ArchiveViewerStateHolder.state.update {
                it.copy(isLoading = false, error = e.message ?: "重新读取失败")
            }
        }
    }
}

fun MainViewModel.archiveDeleteEntry(entry: ArchiveEntry) {
    val state = ArchiveViewerStateHolder.state.value
    val archive = File(state.archivePath)
    if (!ArchiveEditor.supports(archive)) {
        showToast("目前只支持 ZIP 包内删除")
        return
    }
    viewModelScope.launch {
        ArchiveViewerStateHolder.state.update { it.copy(isLoading = true) }
        val r = ArchiveEditor.rewrite(archive, remove = setOf(entry.path))
        r.onSuccess { count ->
            AppLogger.i("ArchiveEdit", "删除条目 ${entry.path}，剩余 $count 条")
            showToast("已删除：${entry.name}")
            reloadArchiveViewer(state.archivePath)
        }.onFailure { e ->
            ArchiveViewerStateHolder.state.update { it.copy(isLoading = false) }
            showToast("删除失败：${e.message}")
        }
    }
}

fun MainViewModel.archiveRenameEntry(entry: ArchiveEntry, newName: String) {
    val state = ArchiveViewerStateHolder.state.value
    val archive = File(state.archivePath)
    val clean = newName.trim().replace("/", "_")
    if (clean.isEmpty()) {
        showToast("名字不能为空")
        return
    }
    if (!ArchiveEditor.supports(archive)) {
        showToast("目前只支持 ZIP 包内重命名")
        return
    }
    val oldPath = entry.path
    val parent = oldPath.substringBeforeLast('/', "")
    val newPath = if (parent.isEmpty()) clean else "$parent/$clean"
    viewModelScope.launch {
        ArchiveViewerStateHolder.state.update { it.copy(isLoading = true) }
        val r = ArchiveEditor.rewrite(archive, rename = mapOf(oldPath to newPath))
        r.onSuccess {
            AppLogger.i("ArchiveEdit", "重命名 $oldPath -> $newPath")
            showToast("已重命名为：$clean")
            reloadArchiveViewer(state.archivePath)
        }.onFailure { e ->
            ArchiveViewerStateHolder.state.update { it.copy(isLoading = false) }
            showToast("重命名失败：${e.message}")
        }
    }
}

/** 把系统文件选择器选中的文件加进当前所在目录 */
fun MainViewModel.archiveAddFiles(uris: List<android.net.Uri>) {
    if (uris.isEmpty()) return
    val state = ArchiveViewerStateHolder.state.value
    val archive = File(state.archivePath)
    if (!ArchiveEditor.supports(archive)) {
        showToast("目前只支持往 ZIP 里添加文件")
        return
    }
    val dir = state.currentDir
    viewModelScope.launch {
        ArchiveViewerStateHolder.state.update { it.copy(isLoading = true) }
        val staged = ArrayList<Pair<File, String>>()
        val cacheDir = File(getApplication<android.app.Application>().cacheDir, "archive_edit").apply { mkdirs() }
        try {
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    val name = queryDisplayName(uri) ?: "file_${System.currentTimeMillis()}"
                    val tmp = File(cacheDir, name)
                    getApplication<android.app.Application>().contentResolver.openInputStream(uri)?.use { input ->
                        tmp.outputStream().use { input.copyTo(it) }
                    }
                    staged.add(tmp to "$dir$name")
                }
            }
            val r = ArchiveEditor.rewrite(archive, add = staged)
            r.onSuccess {
                AppLogger.i("ArchiveEdit", "添加 ${staged.size} 个文件到 ${archive.name}")
                showToast("已添加 ${staged.size} 个文件")
                reloadArchiveViewer(state.archivePath)
            }.onFailure { e ->
                ArchiveViewerStateHolder.state.update { it.copy(isLoading = false) }
                showToast("添加失败：${e.message}")
            }
        } finally {
            staged.forEach { try { it.first.delete() } catch (_: Exception) {} }
        }
    }
}

private fun MainViewModel.queryDisplayName(uri: android.net.Uri): String? = try {
    getApplication<android.app.Application>().contentResolver
        .query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
} catch (_: Exception) {
    null
}
