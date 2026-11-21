package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveEngine
import com.ertyu.engine.RecycleBinManager
import com.ertyu.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

// ==================== 全局状态声明 ====================
val _fileConflictState = MutableStateFlow(FileConflictState())
val MainViewModel.fileConflictState: StateFlow<FileConflictState> get() = _fileConflictState

val _fileClipboardState = MutableStateFlow(FileClipboardState())
val MainViewModel.fileClipboardState: StateFlow<FileClipboardState> get() = _fileClipboardState

// ==================== 基础文件操作 ====================
fun MainViewModel.requestDelete(isLeft: Boolean, item: FileItem) { 
    _deleteTargetIsLeft.value = isLeft; _deleteTarget.value = item 
}

fun MainViewModel.dismissDeleteDialog() { _deleteTarget.value = null }

fun MainViewModel.confirmDelete(useTrash: Boolean) {
    val item = _deleteTarget.value ?: return
    val isLeft = _deleteTargetIsLeft.value
    _deleteTarget.value = null
    viewModelScope.launch {
        AppLogger.i("FileOps", "删除请求: ${item.name}, 使用回收站: $useTrash")
        val path = item.uri.path
        if (useTrash && path != null) {
            val ok = RecycleBinManager.moveToTrash(getApplication(), File(path), item.isDirectory, item.size)
            if (ok) { AppLogger.i("FileOps", "已移入回收站: ${item.name}"); _toast.value = "已移入回收站" }
            else { AppLogger.e("FileOps", "移入回收站失败，尝试直接删除: ${item.name}"); engine.delete(item.uri) }
        } else {
            engine.delete(item.uri)
            AppLogger.i("FileOps", "直接删除: ${item.name}")
            _toast.value = "已直接删除"
        }
        refresh(isLeft)
    }
}

private fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec < 1024) return "$bytesPerSec B/s"
    val kb = bytesPerSec / 1024.0
    if (kb < 1024) return String.format(java.util.Locale.ROOT, "%.1f KB/s", kb)
    return String.format(java.util.Locale.ROOT, "%.1f MB/s", kb / 1024.0)
}

// ==================== 剪贴板操作 ====================
fun MainViewModel.copyToClipboard(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    _fileClipboardState.value = FileClipboardState(targets, isCut = false, sourceIsLeft = isLeft)
    _toast.value = "已复制 ${targets.size} 项"
    clearSelection(isLeft)
}

fun MainViewModel.cutToClipboard(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    _fileClipboardState.value = FileClipboardState(targets, isCut = true, sourceIsLeft = isLeft)
    _toast.value = "已剪切 ${targets.size} 项"
    clearSelection(isLeft)
}

fun MainViewModel.pasteFromClipboard(targetIsLeft: Boolean) {
    val clip = _fileClipboardState.value
    if (clip.isEmpty) return
    val targetUri = flowOf(targetIsLeft).value.currentUri ?: return
    AppLogger.i("Clipboard", "准备从剪贴板粘贴，目标: $targetUri")
    
    val targetNames = flowOf(targetIsLeft).value.items.map { it.name }.toMutableSet()
    val conflicts = clip.items.filter { targetNames.contains(it.name) }
    
    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(
            visible = true, conflictingNames = conflicts.map { it.name },
            onResolve = { action ->
                if (action != ConflictAction.CANCEL) {
                    executePaste(clip.items, targetUri, targetIsLeft, targetNames, action, clip.isCut)
                    _fileClipboardState.value = FileClipboardState()
                }
            }
        )
    } else {
        executePaste(clip.items, targetUri, targetIsLeft, targetNames, ConflictAction.OVERWRITE, clip.isCut)
        _fileClipboardState.value = FileClipboardState()
    }
}

private fun MainViewModel.executePaste(
    targets: List<FileItem>, targetUri: Uri, targetIsLeft: Boolean,
    targetNames: MutableSet<String>, action: ConflictAction, isMove: Boolean
) {
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = if(isMove) "移动中" else "复制中", totalBytes = targets.sumOf { it.size })
        var totalCopied = 0L
        var skipCount = 0

        for (item in targets) {
            val conflictItem = flowOf(targetIsLeft).value.items.find { it.name == item.name }
            if (conflictItem != null) {
                when (action) {
                    ConflictAction.SKIP -> { skipCount++; continue }
                    ConflictAction.OVERWRITE -> { engine.delete(conflictItem.uri); targetNames.remove(conflictItem.name) }
                    ConflictAction.KEEP_BOTH -> {
                        val newName = generateUniqueFileName(conflictItem.name, targetNames)
                        engine.rename(conflictItem.uri, newName)
                        targetNames.remove(conflictItem.name); targetNames.add(newName)
                    }
                    else -> {}
                }
            }
            val ok = if (isMove) engine.move(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = totalCopied + p.copied, speedText = formatSpeed(p.speed)) } }
                     else engine.copy(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = totalCopied + p.copied, speedText = formatSpeed(p.speed)) } }
            if (ok) { totalCopied += item.size; targetNames.add(item.name) }
        }
        _fileTransferState.update { it.copy(isDone = true) }
        refresh(targetIsLeft)
        if (isMove) refresh(!targetIsLeft)
        if (skipCount > 0) _toast.value = "已跳过 $skipCount 项同名文件"
        clearConflictState()
    }
}

// ==================== 对侧复制/移动 ====================
fun MainViewModel.copyToOtherPanel(fromLeft: Boolean, item: FileItem) {
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "复制中", currentFile = item.name)
        val ok = engine.copy(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = p.copied, totalBytes = p.total, speedText = formatSpeed(p.speed)) } }
        if (ok) { _fileTransferState.update { it.copy(isDone = true) }; openDirectory(!fromLeft, targetUri) }
        else { _fileTransferState.update { it.copy(error = "复制失败") } }
    }
}

fun MainViewModel.moveToOtherPanel(fromLeft: Boolean, item: FileItem) {
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "移动中", currentFile = item.name)
        val ok = engine.move(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = p.copied, totalBytes = p.total, speedText = formatSpeed(p.speed)) } }
        if (ok) { _fileTransferState.update { it.copy(isDone = true) }; refresh(fromLeft); openDirectory(!fromLeft, targetUri) }
        else { _fileTransferState.update { it.copy(error = "移动失败") } }
    }
}

fun MainViewModel.dismissTransferDialog() { _fileTransferState.value = FileTransferState() }

// ==================== 批量复制/移动 ====================
fun MainViewModel.batchCopy(fromLeft: Boolean) {
    val state = flowOf(fromLeft).value
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    
    val targetNames = flowOf(!fromLeft).value.items.map { it.name }.toMutableSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(visible = true, conflictingNames = conflicts.map { it.name }, onResolve = { action ->
            if (action != ConflictAction.CANCEL) executeBatchCopy(fromLeft, targets, targetUri, targetNames, action) else { clearSelection(fromLeft); clearConflictState() }
        })
    } else { executeBatchCopy(fromLeft, targets, targetUri, targetNames, ConflictAction.OVERWRITE) }
}

private fun MainViewModel.executeBatchCopy(fromLeft: Boolean, targets: List<FileItem>, targetUri: Uri, targetNames: MutableSet<String>, action: ConflictAction) {
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "批量复制中", totalBytes = targets.sumOf { it.size })
        var totalCopied = 0L
        for (item in targets) {
            val conflictItem = flowOf(!fromLeft).value.items.find { it.name == item.name }
            if (conflictItem != null) {
                when (action) {
                    ConflictAction.SKIP -> continue
                    ConflictAction.OVERWRITE -> { engine.delete(conflictItem.uri); targetNames.remove(conflictItem.name) }
                    ConflictAction.KEEP_BOTH -> {
                        val newName = generateUniqueFileName(conflictItem.name, targetNames)
                        engine.rename(conflictItem.uri, newName); targetNames.remove(conflictItem.name); targetNames.add(newName)
                    }
                    else -> {}
                }
            }
            val ok = engine.copy(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = totalCopied + p.copied, speedText = formatSpeed(p.speed)) } }
            if (ok) { totalCopied += item.size; targetNames.add(item.name) }
        }
        _fileTransferState.update { it.copy(isDone = true) }
        clearSelection(fromLeft); openDirectory(!fromLeft, targetUri); clearConflictState()
    }
}

fun MainViewModel.batchMove(fromLeft: Boolean) {
    val state = flowOf(fromLeft).value
    val targetUri = flowOf(!fromLeft).value.currentUri ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    
    val targetNames = flowOf(!fromLeft).value.items.map { it.name }.toMutableSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(visible = true, conflictingNames = conflicts.map { it.name }, onResolve = { action ->
            if (action != ConflictAction.CANCEL) executeBatchMove(fromLeft, targets, targetUri, targetNames, action) else { clearSelection(fromLeft); clearConflictState() }
        })
    } else { executeBatchMove(fromLeft, targets, targetUri, targetNames, ConflictAction.OVERWRITE) }
}

private fun MainViewModel.executeBatchMove(fromLeft: Boolean, targets: List<FileItem>, targetUri: Uri, targetNames: MutableSet<String>, action: ConflictAction) {
    viewModelScope.launch {
        _fileTransferState.value = FileTransferState(visible = true, title = "批量移动中", totalBytes = targets.sumOf { it.size })
        var totalCopied = 0L
        for (item in targets) {
            val conflictItem = flowOf(!fromLeft).value.items.find { it.name == item.name }
            if (conflictItem != null) {
                when (action) {
                    ConflictAction.SKIP -> continue
                    ConflictAction.OVERWRITE -> { engine.delete(conflictItem.uri); targetNames.remove(conflictItem.name) }
                    ConflictAction.KEEP_BOTH -> {
                        val newName = generateUniqueFileName(conflictItem.name, targetNames)
                        engine.rename(conflictItem.uri, newName); targetNames.remove(conflictItem.name); targetNames.add(newName)
                    }
                    else -> {}
                }
            }
            val ok = engine.move(item.uri, targetUri) { p -> _fileTransferState.update { it.copy(copiedBytes = totalCopied + p.copied, speedText = formatSpeed(p.speed)) } }
            if (ok) { totalCopied += item.size; targetNames.add(item.name) }
        }
        _fileTransferState.update { it.copy(isDone = true) }
        clearSelection(fromLeft); refresh(fromLeft); openDirectory(!fromLeft, targetUri); clearConflictState()
    }
}

// ==================== 压缩与解压基础方法 ====================
fun MainViewModel.requestCompress(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    if (state.selectedUris.isEmpty()) return
    _compressPanelIsLeft.value = isLeft
    _compressPromptVisible.value = true
}

fun MainViewModel.dismissCompressPrompt() { _compressPromptVisible.value = false }

fun MainViewModel.compressWithFormat(format: ArchiveFormat) {
    _compressPromptVisible.value = false
    if (!format.canCompress) return
    val isLeft = _compressPanelIsLeft.value
    val state = flowOf(isLeft).value
    val currentDir = state.currentUri?.path?.let(::File) ?: return
    val sources = state.items.filter { state.selectedUris.contains(it.uriString) }.mapNotNull { it.uri.path?.let(::File) }
    if (sources.isEmpty()) return

    val ext = when (format) { ArchiveFormat.ZIP -> ".zip"; ArchiveFormat.TAR -> ".tar"; ArchiveFormat.TAR_GZ -> ".tar.gz"; ArchiveFormat.SEVEN_Z -> ".7z"; else -> ".zip" }
    val fileName = if (sources.size == 1) { val base = if (sources[0].name.contains('.')) sources[0].name.substringBeforeLast('.') else sources[0].name; "$base$ext" } else { "archive_${System.currentTimeMillis()}$ext" }
    
    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "压缩中", message = fileName)
        val result = ArchiveEngine.compress(sources, File(currentDir, fileName), format) { cur, total -> _zipProgress.value = _zipProgress.value.copy(current = cur, total = total) }
        result.onSuccess { _zipProgress.value = _zipProgress.value.copy(done = true, message = "压缩完成：$fileName") }.onFailure { e -> _zipProgress.value = _zipProgress.value.copy(error = "压缩失败: ${e.message ?: "未知错误"}") }
        clearSelection(isLeft); refresh(isLeft)
    }
}

fun MainViewModel.dismissZipPrompt() { _zipPrompt.value = ZipPrompt() }
fun MainViewModel.dismissZipProgress() { _zipProgress.value = ZipProgress() }

fun MainViewModel.extractZipHere(isLeft: Boolean, item: FileItem, password: String? = null) {
    _zipPrompt.value = ZipPrompt()
    val currentDir = flowOf(isLeft).value.currentUri?.path?.let(::File) ?: return
    val archiveFile = item.uri.path?.let(::File) ?: return
    val targetDir = File(currentDir, archiveBaseName(archiveFile.name))
    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "解压中", message = archiveFile.name)
        val result = ArchiveEngine.extract(archiveFile, targetDir, password) { cur, total -> _zipProgress.value = _zipProgress.value.copy(current = cur, total = total) }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "解压完成：${targetDir.name}")
            refresh(isLeft); openDirectory(isLeft, Uri.fromFile(targetDir))
        }.onFailure { e ->
            if (e.message == "PASSWORD_REQUIRED") {
                _zipProgress.value = _zipProgress.value.copy(needsPassword = true, error = "文件已加密", pendingFileItem = item, pendingIsLeft = isLeft, pendingTargetIsLeft = isLeft)
            } else { _zipProgress.value = _zipProgress.value.copy(error = "解压失败: ${e.message ?: "未知错误"}") }
        }
    }
}

fun MainViewModel.extractZipToOtherPanel(isLeft: Boolean, item: FileItem, password: String? = null) {
    _zipPrompt.value = ZipPrompt()
    val targetDir = flowOf(!isLeft).value.currentUri?.path?.let(::File) ?: return
    val archiveFile = item.uri.path?.let(::File) ?: return
    val finalDir = File(targetDir, archiveBaseName(archiveFile.name))
    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "解压中", message = archiveFile.name)
        val result = ArchiveEngine.extract(archiveFile, finalDir, password) { cur, total -> _zipProgress.value = _zipProgress.value.copy(current = cur, total = total) }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "解压完成：${finalDir.name}")
            openDirectory(!isLeft, Uri.fromFile(targetDir)); openDirectory(!isLeft, Uri.fromFile(finalDir))
        }.onFailure { e ->
            if (e.message == "PASSWORD_REQUIRED") {
                _zipProgress.value = _zipProgress.value.copy(needsPassword = true, error = "文件已加密", pendingFileItem = item, pendingIsLeft = isLeft, pendingTargetIsLeft = !isLeft)
            } else { _zipProgress.value = _zipProgress.value.copy(error = "解压失败: ${e.message ?: "未知错误"}") }
        }
    }
}

fun MainViewModel.submitZipPassword(password: String) {
    val state = _zipProgress.value
    val item = state.pendingFileItem ?: return
    val isLeft = state.pendingIsLeft
    val targetIsLeft = state.pendingTargetIsLeft
    _zipProgress.update { it.copy(needsPassword = false, error = null) }
    if (isLeft == targetIsLeft) extractZipHere(isLeft, item, password) else extractZipToOtherPanel(isLeft, item, password)
}

private fun archiveBaseName(fileName: String): String {
    val lower = fileName.lowercase()
    return when {
        lower.endsWith(".tar.gz") -> fileName.dropLast(7); lower.endsWith(".tar.bz2") -> fileName.dropLast(8)
        lower.endsWith(".tar.xz") -> fileName.dropLast(7); lower.endsWith(".tar.zst") -> fileName.dropLast(8)
        lower.endsWith(".tar") -> fileName.dropLast(4); lower.endsWith(".tgz") -> fileName.dropLast(4)
        lower.endsWith(".tbz2") -> fileName.dropLast(5); lower.endsWith(".tbz") -> fileName.dropLast(4)
        lower.endsWith(".txz") -> fileName.dropLast(4); lower.endsWith(".tzst") -> fileName.dropLast(5)
        else -> fileName.substringBeforeLast('.', fileName)
    }
}

// ==================== 弹窗控制 ====================
fun MainViewModel.createDirectory(isLeft: Boolean, name: String) {
    val current = flowOf(isLeft).value.currentUri ?: return
    viewModelScope.launch { engine.createDirectory(current, name); refresh(isLeft) }
}
fun MainViewModel.rename(isLeft: Boolean, item: FileItem, newName: String) {
    viewModelScope.launch { engine.rename(item.uri, newName); refresh(isLeft) }
}
fun MainViewModel.createFile(name: String) {
    _newFilePrompt.value = false
    val isLeft = _newFilePanelIsLeft.value
    val dir = flowOf(isLeft).value.currentUri?.path?.let(::File) ?: return
    viewModelScope.launch {
        try {
            val f = File(dir, name)
            if (f.exists()) { _toast.value = "文件已存在"; return@launch }
            f.parentFile?.mkdirs(); f.createNewFile(); _toast.value = "已创建：${f.name}"; refresh(isLeft)
        } catch (e: Exception) { _toast.value = "创建失败：${e.message}" }
    }
}
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