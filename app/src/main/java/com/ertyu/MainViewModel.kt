package com.ertyu

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ArchiveEngine
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.FileEngine
import com.ertyu.engine.ZipEngine
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.EditorState
import com.ertyu.model.FileItem
import com.ertyu.model.ImageViewerState
import com.ertyu.model.PanelState
import com.ertyu.model.SortMode
import com.ertyu.model.ZipProgress
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val engine: FileEngine = DirectFileEngine()

    private val _leftPanel = MutableStateFlow(PanelState())
    val leftPanel: StateFlow<PanelState> = _leftPanel.asStateFlow()

    private val _rightPanel = MutableStateFlow(PanelState())
    val rightPanel: StateFlow<PanelState> = _rightPanel.asStateFlow()

    private val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    private val _imageViewer = MutableStateFlow(ImageViewerState())
    val imageViewer: StateFlow<ImageViewerState> = _imageViewer.asStateFlow()

    private val _zipProgress = MutableStateFlow(ZipProgress())
    val zipProgress: StateFlow<ZipProgress> = _zipProgress.asStateFlow()

    private val _zipPrompt = MutableStateFlow(ZipPrompt())
    val zipPrompt: StateFlow<ZipPrompt> = _zipPrompt.asStateFlow()

    private fun flowOf(isLeft: Boolean) = if (isLeft) _leftPanel else _rightPanel

    fun initDefaultDirs() {
        val root = Uri.fromFile(Environment.getExternalStorageDirectory())
        openDirectory(true, root)
        viewModelScope.launch {
            delay(150)
            openDirectory(false, root)
        }
    }

    fun openDirectory(isLeft: Boolean, uri: Uri) {
        val flow = flowOf(isLeft)
        flow.update {
            it.copy(
                isLoading = true,
                error = null,
                selectedUris = emptySet(),
                isSelectionMode = false
            )
        }
        viewModelScope.launch {
            try {
                val all = engine.list(uri)
                val parent = engine.getParent(uri)
                flow.update { state ->
                    state.copy(
                        currentUri = uri,
                        rawItems = all,
                        items = process(all, state.sortMode, state.showHidden),
                        isLoading = false,
                        canGoUp = parent != null
                    )
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun process(
        raw: List<FileItem>,
        sort: SortMode,
        showHidden: Boolean
    ): List<FileItem> {
        val filtered = if (showHidden) raw else raw.filter { !it.name.startsWith(".") }
        val comparator = when (sort) {
            SortMode.NAME_ASC ->
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            SortMode.NAME_DESC ->
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC ->
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
            SortMode.SIZE_DESC ->
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
            SortMode.TIME_ASC ->
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified }
            SortMode.TIME_DESC ->
                compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortMode.TYPE_ASC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.substringAfterLast('.', "").lowercase() }
            SortMode.TYPE_DESC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenByDescending { it.name.substringAfterLast('.', "").lowercase() }
        }
        return filtered.sortedWith(comparator)
    }

    fun setSortMode(isLeft: Boolean, mode: SortMode) {
        flowOf(isLeft).update {
            it.copy(sortMode = mode, items = process(it.rawItems, mode, it.showHidden))
        }
    }

    fun setShowHidden(isLeft: Boolean, show: Boolean) {
        flowOf(isLeft).update {
            it.copy(showHidden = show, items = process(it.rawItems, it.sortMode, show))
        }
    }

    fun refresh(isLeft: Boolean) {
        flowOf(isLeft).value.currentUri?.let { openDirectory(isLeft, it) }
    }

    fun goUp(isLeft: Boolean) {
        val flow = flowOf(isLeft)
        val current = flow.value.currentUri ?: return
        val parent = engine.getParent(current) ?: return
        openDirectory(isLeft, parent)
    }

    fun delete(isLeft: Boolean, item: FileItem) {
        viewModelScope.launch {
            engine.delete(item.uri)
            refresh(isLeft)
        }
    }

    fun copyToOtherPanel(fromLeft: Boolean, item: FileItem) {
        val targetUri = flowOf(!fromLeft).value.currentUri ?: return
        viewModelScope.launch {
            engine.copy(item.uri, targetUri)
            openDirectory(!fromLeft, targetUri)
        }
    }

    fun moveToOtherPanel(fromLeft: Boolean, item: FileItem) {
        val targetUri = flowOf(!fromLeft).value.currentUri ?: return
        viewModelScope.launch {
            engine.move(item.uri, targetUri)
            refresh(fromLeft)
            openDirectory(!fromLeft, targetUri)
        }
    }

    fun createDirectory(isLeft: Boolean, name: String) {
        val current = flowOf(isLeft).value.currentUri ?: return
        viewModelScope.launch {
            engine.createDirectory(current, name)
            refresh(isLeft)
        }
    }

    fun rename(isLeft: Boolean, item: FileItem, newName: String) {
        viewModelScope.launch {
            engine.rename(item.uri, newName)
            refresh(isLeft)
        }
    }

    // ================= 多选 =================

    fun enterSelection(isLeft: Boolean, uriString: String) {
        flowOf(isLeft).update {
            it.copy(selectedUris = setOf(uriString), isSelectionMode = true)
        }
    }

    fun toggleSelection(isLeft: Boolean, uriString: String) {
        flowOf(isLeft).update { state ->
            val set = state.selectedUris.toMutableSet()
            if (!set.add(uriString)) set.remove(uriString)
            state.copy(selectedUris = set, isSelectionMode = set.isNotEmpty())
        }
    }

    fun selectAll(isLeft: Boolean) {
        flowOf(isLeft).update { state ->
            state.copy(
                selectedUris = state.items.map { it.uriString }.toSet(),
                isSelectionMode = true
            )
        }
    }

    fun clearSelection(isLeft: Boolean) {
        flowOf(isLeft).update {
            it.copy(selectedUris = emptySet(), isSelectionMode = false)
        }
    }

    fun batchDelete(isLeft: Boolean) {
        val state = flowOf(isLeft).value
        val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
        viewModelScope.launch {
            targets.forEach { engine.delete(it.uri) }
            clearSelection(isLeft)
            refresh(isLeft)
        }
    }

    fun batchCopy(fromLeft: Boolean) {
        val state = flowOf(fromLeft).value
        val target = flowOf(!fromLeft).value.currentUri ?: return
        val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
        viewModelScope.launch {
            targets.forEach { engine.copy(it.uri, target) }
            clearSelection(fromLeft)
            openDirectory(!fromLeft, target)
        }
    }

    fun batchMove(fromLeft: Boolean) {
        val state = flowOf(fromLeft).value
        val target = flowOf(!fromLeft).value.currentUri ?: return
        val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
        viewModelScope.launch {
            targets.forEach { engine.move(it.uri, target) }
            clearSelection(fromLeft)
            refresh(fromLeft)
            openDirectory(!fromLeft, target)
        }
    }

    // ================= 打开文件 =================

    fun openFile(item: FileItem) {
        val ext = item.name.substringAfterLast('.', "").lowercase()
        when {
            // 任何压缩包格式，弹解压对话框
            ArchiveFormat.isArchive(item.name) -> _zipPrompt.value = ZipPrompt(item)
            ext in TEXT_EXTS -> openInEditor(item)
            ext in IMAGE_EXTS -> openInImageViewer(item)
        }
    }

    private fun openInEditor(item: FileItem) {
        _editor.update {
            it.copy(
                isOpen = true,
                uri = item.uri,
                fileName = item.name,
                content = "",
                originalContent = "",
                isLoading = true,
                error = null
            )
        }
        viewModelScope.launch {
            val text = engine.readText(item.uri)
            _editor.update { s ->
                if (text == null) {
                    s.copy(isLoading = false, error = "无法读取文件")
                } else {
                    s.copy(content = text, originalContent = text, isLoading = false)
                }
            }
        }
    }

    private fun openInImageViewer(item: FileItem) {
        _imageViewer.update {
            it.copy(isOpen = true, uri = item.uri, fileName = item.name)
        }
    }

    fun updateEditorContent(text: String) {
        _editor.update { it.copy(content = text) }
    }

    fun saveEditor() {
        val s = _editor.value
        val uri = s.uri ?: return
        viewModelScope.launch {
            val ok = engine.writeText(uri, s.content)
            if (ok) {
                _editor.update { it.copy(originalContent = it.content) }
            } else {
                _editor.update { it.copy(error = "保存失败") }
            }
        }
    }

    fun closeEditor() {
        _editor.value = EditorState()
    }

    fun closeImageViewer() {
        _imageViewer.value = ImageViewerState()
    }

    // ================= 压缩包 =================

    fun dismissZipPrompt() {
        _zipPrompt.value = ZipPrompt()
    }

    fun extractZipHere(isLeft: Boolean, item: FileItem) {
        _zipPrompt.value = ZipPrompt()
        val currentDir = flowOf(isLeft).value.currentUri?.path?.let { File(it) } ?: return
        val archiveFile = item.uri.path?.let { File(it) } ?: return
        // 去掉扩展名作为目录名
        val baseName = archiveBaseName(archiveFile.name)
        val targetDir = File(currentDir, baseName)

        viewModelScope.launch {
            _zipProgress.value = ZipProgress(
                visible = true,
                title = "解压中",
                message = archiveFile.name
            )
            val result = ArchiveEngine.extract(archiveFile, targetDir) { cur, total ->
                _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
            }
            result.onSuccess {
                _zipProgress.value = _zipProgress.value.copy(
                    done = true,
                    message = "解压完成：${targetDir.name}"
                )
            }.onFailure { e ->
                _zipProgress.value = _zipProgress.value.copy(
                    error = e.message ?: "解压失败"
                )
            }
            refresh(isLeft)
        }
    }

    fun extractZipToOtherPanel(isLeft: Boolean, item: FileItem) {
        _zipPrompt.value = ZipPrompt()
        val targetDir = flowOf(!isLeft).value.currentUri?.path?.let { File(it) } ?: return
        val archiveFile = item.uri.path?.let { File(it) } ?: return
        val baseName = archiveBaseName(archiveFile.name)
        val finalDir = File(targetDir, baseName)

        viewModelScope.launch {
            _zipProgress.value = ZipProgress(
                visible = true,
                title = "解压中",
                message = archiveFile.name
            )
            val result = ArchiveEngine.extract(archiveFile, finalDir) { cur, total ->
                _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
            }
            result.onSuccess {
                _zipProgress.value = _zipProgress.value.copy(
                    done = true,
                    message = "解压完成：${finalDir.name}"
                )
            }.onFailure { e ->
                _zipProgress.value = _zipProgress.value.copy(
                    error = e.message ?: "解压失败"
                )
            }
            openDirectory(!isLeft, Uri.fromFile(targetDir))
        }
    }

    /** 根据压缩包名推断解压后的目录名 */
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

    /** 把当前选中的文件/文件夹压缩为 ZIP */
    fun compressSelection(isLeft: Boolean) {
        val state = flowOf(isLeft).value
        val currentDir = state.currentUri?.path?.let { File(it) } ?: return
        val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
        if (targets.isEmpty()) return

        val sources = targets.mapNotNull { it.uri.path?.let(::File) }
        if (sources.isEmpty()) return

        val zipName = if (sources.size == 1) {
            val n = sources[0].name
            val base = if (n.contains('.')) n.substringBeforeLast('.') else n
            "$base.zip"
        } else {
            "archive_${System.currentTimeMillis()}.zip"
        }
        val targetZip = File(currentDir, zipName)

        viewModelScope.launch {
            _zipProgress.value = ZipProgress(
                visible = true,
                title = "压缩中",
                message = zipName
            )
            val result = ZipEngine.compress(sources, targetZip) { cur, total ->
                _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
            }
            result.onSuccess {
                _zipProgress.value = _zipProgress.value.copy(
                    done = true,
                    message = "压缩完成：$zipName"
                )
            }.onFailure { e ->
                _zipProgress.value = _zipProgress.value.copy(
                    error = e.message ?: "压缩失败"
                )
            }
            clearSelection(isLeft)
            refresh(isLeft)
        }
    }

    fun dismissZipProgress() {
        _zipProgress.value = ZipProgress()
    }

    companion object {
        private val TEXT_EXTS = setOf(
            "txt", "md", "json", "xml", "kt", "java", "kts",
            "gradle", "properties", "yml", "yaml", "html", "css", "js",
            "log", "ini", "conf", "cfg", "sh", "bat"
        )
        private val IMAGE_EXTS = setOf(
            "jpg", "jpeg", "png", "gif", "bmp", "webp"
        )
    }
}