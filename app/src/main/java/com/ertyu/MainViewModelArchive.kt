package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ArchiveEngine
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.FileItem
import com.ertyu.model.ZipProgress
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.dismissZipPrompt() {
    _zipPrompt.value = ZipPrompt()
}

fun MainViewModel.extractZipHere(isLeft: Boolean, item: FileItem, password: String? = null) {
    _zipPrompt.value = ZipPrompt()
    val currentDir = flowOf(isLeft).value.currentUri?.path?.let(::File) ?: return
    val archiveFile = item.uri.path?.let(::File) ?: return
    val targetDir = File(currentDir, archiveBaseName(archiveFile.name))

    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "解压中", message = archiveFile.name)
        val result = ArchiveEngine.extract(archiveFile, targetDir, password) { cur, total ->
            _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
        }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "解压完成：${targetDir.name}")
            refresh(isLeft)
            // 自动打开解压出来的目录
            openDirectory(isLeft, Uri.fromFile(targetDir))
        }.onFailure { e ->
            if (e.message == "PASSWORD_REQUIRED") {
                _zipProgress.value = _zipProgress.value.copy(
                    needsPassword = true, 
                    error = "文件已加密",
                    pendingFileItem = item,
                    pendingIsLeft = isLeft,
                    pendingTargetIsLeft = isLeft
                )
            } else {
                _zipProgress.value = _zipProgress.value.copy(error = "解压失败: ${e.message ?: "未知错误"}")
            }
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
        val result = ArchiveEngine.extract(archiveFile, finalDir, password) { cur, total ->
            _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
        }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "解压完成：${finalDir.name}")
            openDirectory(!isLeft, Uri.fromFile(targetDir))
            // 自动跳转进解压目录
            openDirectory(!isLeft, Uri.fromFile(finalDir))
        }.onFailure { e ->
            if (e.message == "PASSWORD_REQUIRED") {
                _zipProgress.value = _zipProgress.value.copy(
                    needsPassword = true, 
                    error = "文件已加密",
                    pendingFileItem = item,
                    pendingIsLeft = isLeft,
                    pendingTargetIsLeft = !isLeft
                )
            } else {
                _zipProgress.value = _zipProgress.value.copy(error = "解压失败: ${e.message ?: "未知错误"}")
            }
        }
    }
}

fun MainViewModel.submitZipPassword(password: String) {
    val state = _zipProgress.value
    val item = state.pendingFileItem ?: return
    val isLeft = state.pendingIsLeft
    val targetIsLeft = state.pendingTargetIsLeft
    
    _zipProgress.update { it.copy(needsPassword = false, error = null) }
    
    if (isLeft == targetIsLeft) {
        extractZipHere(isLeft, item, password)
    } else {
        extractZipToOtherPanel(isLeft, item, password)
    }
}

private fun archiveBaseName(fileName: String): String {
    val lower = fileName.lowercase()
    return when {
        lower.endsWith(".tar.gz") -> fileName.dropLast(7)
        lower.endsWith(".tar.bz2") -> fileName.dropLast(8)
        lower.endsWith(".tar.xz") -> fileName.dropLast(7)
        lower.endsWith(".tar.zst") -> fileName.dropLast(8)
        lower.endsWith(".tar") -> fileName.dropLast(4)
        lower.endsWith(".tgz") -> fileName.dropLast(4)
        lower.endsWith(".tbz2") -> fileName.dropLast(5)
        lower.endsWith(".tbz") -> fileName.dropLast(4)
        lower.endsWith(".txz") -> fileName.dropLast(4)
        lower.endsWith(".tzst") -> fileName.dropLast(5)
        else -> fileName.substringBeforeLast('.', fileName)
    }
}

fun MainViewModel.requestCompress(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    if (state.selectedUris.isEmpty()) return
    _compressPanelIsLeft.value = isLeft
    _compressPromptVisible.value = true
}

fun MainViewModel.dismissCompressPrompt() {
    _compressPromptVisible.value = false
}

fun MainViewModel.compressWithFormat(format: ArchiveFormat) {
    _compressPromptVisible.value = false
    if (!format.canCompress) return
    val isLeft = _compressPanelIsLeft.value
    val state = flowOf(isLeft).value
    val currentDir = state.currentUri?.path?.let(::File) ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    val sources = targets.mapNotNull { it.uri.path?.let(::File) }
    if (sources.isEmpty()) return
    
    val ext = when (format) {
        ArchiveFormat.ZIP -> ".zip"
        ArchiveFormat.TAR -> ".tar"
        ArchiveFormat.TAR_GZ -> ".tar.gz"
        ArchiveFormat.SEVEN_Z -> ".7z"
        else -> ".zip"
    }
    
    val fileName = if (sources.size == 1) {
        val n = sources[0].name
        val base = if (n.contains('.')) n.substringBeforeLast('.') else n
        "$base$ext"
    } else {
        "archive_${System.currentTimeMillis()}$ext"
    }
    
    val targetFile = File(currentDir, fileName)
    
    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "压缩中", message = fileName)
        val result = ArchiveEngine.compress(sources, targetFile, format) { cur, total ->
            _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
        }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "压缩完成：$fileName")
        }.onFailure { e ->
            _zipProgress.value = _zipProgress.value.copy(error = "压缩失败: ${e.message ?: "未知错误"}")
        }
        clearSelection(isLeft)
        refresh(isLeft)
    }
}

fun MainViewModel.dismissZipProgress() {
    _zipProgress.value = ZipProgress()
}