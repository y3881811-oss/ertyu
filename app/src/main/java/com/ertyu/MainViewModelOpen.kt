package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.FolderSizeCalculator
import com.ertyu.model.EditorState
import com.ertyu.model.FileItem
import com.ertyu.model.FolderSizeState
import com.ertyu.model.HexViewerState
import com.ertyu.model.ImageViewerState
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

private val hexLoadChunk = 64 * 1024
private var hexFile: File? = null
private var hexLoadedBytes = 0

fun MainViewModel.openFile(item: FileItem) {
    val ext = item.name.substringAfterLast('.', "").lowercase()
    when {
        ext == "apk" -> openApkInfo(item)
        com.ertyu.model.ArchiveFormat.isArchive(item.name) -> _zipPrompt.value = ZipPrompt(item)
        ext in TEXT_FILE_EXTS -> openInEditor(item)
        ext in IMAGE_FILE_EXTS -> openInImageViewer(item)
    }
}

fun MainViewModel.openInEditor(item: FileItem) {
    _editor.update {
        it.copy(isOpen = true, uri = item.uri, fileName = item.name, content = "", originalContent = "", isLoading = true, error = null)
    }
    viewModelScope.launch {
        val text = engine.readText(item.uri)
        _editor.update { s ->
            if (text == null) s.copy(isLoading = false, error = "无法读取文件")
            else s.copy(content = text, originalContent = text, isLoading = false)
        }
    }
}

fun MainViewModel.openInImageViewer(item: FileItem) {
    _imageViewer.update { it.copy(isOpen = true, uri = item.uri, fileName = item.name) }
}

fun MainViewModel.updateEditorContent(text: String) { _editor.update { it.copy(content = text) } }

fun MainViewModel.saveEditor() {
    val s = _editor.value
    val uri = s.uri ?: return
    viewModelScope.launch {
        val ok = engine.writeText(uri, s.content)
        if (ok) _editor.update { it.copy(originalContent = it.content) }
        else _editor.update { it.copy(error = "保存失败") }
    }
}

fun MainViewModel.closeEditor() { _editor.value = EditorState() }
fun MainViewModel.closeImageViewer() { _imageViewer.value = ImageViewerState() }

fun MainViewModel.openHexViewer(item: FileItem) {
    val file = item.uri.path?.let(::File) ?: run { _toast.value = "无效路径"; return }
    if (!file.exists() || !file.isFile) { _toast.value = "文件不存在"; return }
    hexFile = file
    hexLoadedBytes = 0
    _hexViewerState.value = HexViewerState(visible = true, filePath = file.absolutePath, fileName = file.name, fileSize = file.length(), isLoading = true)
    loadHexChunk(this)
}

fun MainViewModel.loadMoreHex() {
    if (hexFile == null) return
    _hexViewerState.update { it.copy(isLoading = true) }
    loadHexChunk(this)
}

private fun loadHexChunk(vm: MainViewModel) {
    val file = hexFile ?: return
    vm.viewModelScope.launch {
        try {
            val chunk = withContext(Dispatchers.IO) {
                RandomAccessFile(file, "r").use { raf ->
                    val remaining = (file.length() - hexLoadedBytes).coerceAtMost(hexLoadChunk.toLong())
                    if (remaining <= 0) return@withContext ByteArray(0)
                    raf.seek(hexLoadedBytes.toLong())
                    val buf = ByteArray(remaining.toInt())
                    raf.readFully(buf)
                    buf
                }
            }
            val oldBytes = vm._hexViewerState.value.bytes
            val newBytes = oldBytes + chunk
            hexLoadedBytes += chunk.size
            vm._hexViewerState.update { it.copy(bytes = newBytes, loadedBytes = hexLoadedBytes, isLoading = false, error = null) }
        } catch (e: Exception) {
            vm._hexViewerState.update { it.copy(isLoading = false, error = e.message ?: "读取失败") }
        }
    }
}

fun MainViewModel.closeHexViewer() {
    hexFile = null
    hexLoadedBytes = 0
    _hexViewerState.value = HexViewerState()
}

fun MainViewModel.calculateFolderSize(item: FileItem) {
    folderSizeJob?.cancel()
    val folder = item.uri.path?.let(::File) ?: return
    _folderSizeState.value = FolderSizeState(isCalculating = true)
    folderSizeJob = viewModelScope.launch {
        try {
            val (bytes, count) = FolderSizeCalculator.calculate(folder) { scanned, totalBytes ->
                _folderSizeState.value = FolderSizeState(isCalculating = true, scanned = scanned, totalBytes = totalBytes)
            }
            _folderSizeState.value = FolderSizeState(isCalculating = false, scanned = count, totalBytes = bytes, done = true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _folderSizeState.value = FolderSizeState(error = e.message ?: "计算失败")
        }
    }
}

fun MainViewModel.cancelFolderSize() {
    folderSizeJob?.cancel()
    folderSizeJob = null
    _folderSizeState.value = FolderSizeState()
}

fun MainViewModel.resetFolderSize() {
    folderSizeJob?.cancel()
    folderSizeJob = null
    _folderSizeState.value = FolderSizeState()
}