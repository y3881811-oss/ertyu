package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.RecycleBinManager
import com.ertyu.model.FileItem
import com.ertyu.model.FileTransferState
import com.ertyu.model.TransferProgress
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.requestDelete(isLeft: Boolean, item: FileItem) {
    _deleteTargetIsLeft.value = isLeft
    _deleteTarget.value = item
}

fun MainViewModel.dismissDeleteDialog() { _deleteTarget.value = null }

fun MainViewModel.confirmDelete(useTrash: Boolean) {
    val item = _deleteTarget.value ?: return
    val isLeft = _deleteTargetIsLeft.value
    _deleteTarget.value = null

    viewModelScope.launch {
        val path = item.uri.path
        if (useTrash && path != null) {
            val file = File(path)
            val ok = RecycleBinManager.moveToTrash(getApplication(), file, item.isDirectory, item.size)
            if (ok) _toast.value = "已移入回收站"
            else { _toast.value = "移入回收站失败，已直接删除"; engine.delete(item.uri) }
        } else {
            engine.delete(item.uri)
            _toast.value = "已直接删除"
        }
        refresh(isLeft)
    }
}

// ==================== 新增：带进度的复制与移动 ====================

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec < 1024) return "$bytesPerSec B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024) return String.format("%.1f KB/s", kb)
    val mb = kb / 1024.0
    return String.format("%.1f MB/s", mb)
}

fun MainViewModel.copyToOtherPanel(fromLeft: Boolean, item: FileItem) {
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(
            visible = true,
            title = "正在复制",
            currentFile = item.name
        )
        val ok = engine.copy(item.uri, targetUri) { p: TransferProgress ->
            _fileTransferState.update {
                it.copy(
                    currentFile = p.currentFileName,
                    copiedBytes = p.copied,
                    totalBytes = p.total,
                    speedText = formatSpeed(p.speed)
                )
            }
        }
        if (ok) {
            _fileTransferState.update { it.copy(isDone = true) }
            openDirectory(!fromLeft, targetUri)
        } else {
            _fileTransferState.update { it.copy(error = "复制失败") }
        }
    }
}

fun MainViewModel.moveToOtherPanel(fromLeft: Boolean, item: FileItem) {
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(
            visible = true,
            title = "正在移动",
            currentFile = item.name
        )
        val ok = engine.move(item.uri, targetUri) { p: TransferProgress ->
            _fileTransferState.update {
                it.copy(
                    currentFile = p.currentFileName,
                    copiedBytes = p.copied,
                    totalBytes = p.total,
                    speedText = formatSpeed(p.speed)
                )
            }
        }
        if (ok) {
            _fileTransferState.update { it.copy(isDone = true) }
            refresh(fromLeft)
            openDirectory(!fromLeft, targetUri)
        } else {
            _fileTransferState.update { it.copy(error = "移动失败") }
        }
    }
}

fun MainViewModel.dismissTransferDialog() {
    _fileTransferState.value = FileTransferState()
}

// ==================== 批量操作 ====================

fun MainViewModel.batchCopy(fromLeft: Boolean) {
    val state = flowOf(fromLeft).value
    val target = flowOf(!fromLeft).value.currentUri ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "批量复制中", totalBytes = targets.sumOf { it.size })
        var totalCopied = 0L
        val totalSize = targets.sumOf { it.size }
        
        for (item in targets) {
            val ok = engine.copy(item.uri, target) { p: TransferProgress ->
                _fileTransferState.update {
                    it.copy(
                        currentFile = p.currentFileName,
                        copiedBytes = totalCopied + p.copied,
                        speedText = formatSpeed(p.speed)
                    )
                }
            }
            if (ok) totalCopied += item.size
        }
        _fileTransferState.update { it.copy(isDone = true) }
        clearSelection(fromLeft)
        openDirectory(!fromLeft, target)
    }
}

fun MainViewModel.batchMove(fromLeft: Boolean) {
    val state = flowOf(fromLeft).value
    val target = flowOf(!fromLeft).value.currentUri ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "批量移动中", totalBytes = targets.sumOf { it.size })
        var totalCopied = 0L
        
        for (item in targets) {
            val ok = engine.move(item.uri, target) { p: TransferProgress ->
                _fileTransferState.update {
                    it.copy(
                        currentFile = p.currentFileName,
                        copiedBytes = totalCopied + p.copied,
                        speedText = formatSpeed(p.speed)
                    )
                }
            }
            if (ok) totalCopied += item.size
        }
        _fileTransferState.update { it.copy(isDone = true) }
        clearSelection(fromLeft)
        refresh(fromLeft)
        openDirectory(!fromLeft, target)
    }
}

fun MainViewModel.createDirectory(isLeft: Boolean, name: String) {
    val current = flowOf(isLeft).value.currentUri ?: return
    viewModelScope.launch {
        engine.createDirectory(current, name)
        refresh(isLeft)
    }
}

fun MainViewModel.rename(isLeft: Boolean, item: FileItem, newName: String) {
    viewModelScope.launch {
        engine.rename(item.uri, newName)
        refresh(isLeft)
    }
}

fun MainViewModel.createFile(name: String) {
    _newFilePrompt.value = false
    val isLeft = _newFilePanelIsLeft.value
    val dir = flowOf(isLeft).value.currentUri?.path?.let(::File) ?: return
    viewModelScope.launch {
        try {
            val f = File(dir, name)
            if (f.exists()) { _toast.value = "文件已存在"; return@launch }
            f.parentFile?.mkdirs()
            f.createNewFile()
            _toast.value = "已创建：${f.name}"
            refresh(isLeft)
        } catch (e: Exception) {
            _toast.value = "创建失败：${e.message}"
        }
    }
}

// 弹窗控制
fun MainViewModel.openNewFilePrompt(isLeft: Boolean) { _newFilePanelIsLeft.value = isLeft; _newFilePrompt.value = true }
fun MainViewModel.dismissNewFilePrompt() { _newFilePrompt.value = false }
fun MainViewModel.openNewFolderDialog(isLeft: Boolean) { _newFolderDialogIsLeft.value = isLeft; _newFolderDialogVisible.value = true }
fun MainViewModel.dismissNewFolderDialog() { _newFolderDialogVisible.value = false }
fun MainViewModel.openJumpDialog(isLeft: Boolean) { _jumpDialogIsLeft.value = isLeft; _jumpDialogVisible.value = true }
fun MainViewModel.dismissJumpDialog() { _jumpDialogVisible.value = false }
fun MainViewModel.openSortDialog(isLeft: Boolean) { _sortDialogIsLeft.value = isLeft; _sortDialogVisible.value = true }
fun MainViewModel.dismissSortDialog() { _sortDialogVisible.value = false }
fun MainViewModel.openActionDialog(isLeft: Boolean, item: FileItem) { _actionDialogIsLeft.value = isLeft; _actionDialogTarget.value = item }
fun MainViewModel.dismissActionDialog() { _actionDialogTarget.value = null }
fun MainViewModel.openInfoDialog(item: FileItem) { _infoDialogTarget.value = item }
fun MainViewModel.dismissInfoDialog() { _infoDialogTarget.value = null; resetFolderSize() }
fun MainViewModel.openRenameDialog(isLeft: Boolean, item: FileItem) { _renameDialogIsLeft.value = isLeft; _renameDialogTarget.value = item }
fun MainViewModel.dismissRenameDialog() { _renameDialogTarget.value = null }