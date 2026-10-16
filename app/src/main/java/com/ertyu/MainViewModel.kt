package com.ertyu

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.BookmarkStore
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.FileEngine
import com.ertyu.engine.FolderSizeCalculator
import com.ertyu.model.ApkBrowserState
import com.ertyu.model.ApkInfoState
import com.ertyu.model.AppSettings
import com.ertyu.model.BookmarkPanelState
import com.ertyu.model.EditorState
import com.ertyu.model.FileItem
import com.ertyu.model.FolderSizeState
import com.ertyu.model.HexViewerState
import com.ertyu.model.ImageViewerState
import com.ertyu.model.InstallState
import com.ertyu.model.InstalledAppsState
import com.ertyu.model.PanelState
import com.ertyu.model.SearchState
import com.ertyu.model.SortMode
import com.ertyu.model.ZipProgress
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile

internal val TEXT_FILE_EXTS = setOf(
    "txt", "md", "json", "xml", "kt", "java", "kts",
    "gradle", "properties", "yml", "yaml", "html", "css", "js",
    "log", "ini", "conf", "cfg", "sh", "bat"
)

internal val IMAGE_FILE_EXTS = setOf(
    "jpg", "jpeg", "png", "gif", "bmp", "webp"
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    internal val engine: FileEngine = DirectFileEngine()

    // ==================== 面板 / 查看器 ====================

    internal val _leftPanel = MutableStateFlow(PanelState())
    val leftPanel: StateFlow<PanelState> = _leftPanel.asStateFlow()

    internal val _rightPanel = MutableStateFlow(PanelState())
    val rightPanel: StateFlow<PanelState> = _rightPanel.asStateFlow()

    internal val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()

    internal val _imageViewer = MutableStateFlow(ImageViewerState())
    val imageViewer: StateFlow<ImageViewerState> = _imageViewer.asStateFlow()

    internal val _hexViewerState = MutableStateFlow(HexViewerState())
    val hexViewerState: StateFlow<HexViewerState> =
        _hexViewerState.asStateFlow()

    // ==================== 压缩 ====================

    internal val _zipProgress = MutableStateFlow(ZipProgress())
    val zipProgress: StateFlow<ZipProgress> = _zipProgress.asStateFlow()

    internal val _zipPrompt = MutableStateFlow(ZipPrompt())
    val zipPrompt: StateFlow<ZipPrompt> = _zipPrompt.asStateFlow()

    internal val _compressPromptVisible = MutableStateFlow(false)
    val compressPromptVisible: StateFlow<Boolean> =
        _compressPromptVisible.asStateFlow()
    internal val _compressPanelIsLeft = MutableStateFlow(true)

    // ==================== 属性 ====================

    internal val _folderSizeState = MutableStateFlow(FolderSizeState())
    val folderSizeState: StateFlow<FolderSizeState> =
        _folderSizeState.asStateFlow()

    // ==================== 搜索 / 书签 ====================

    internal val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    internal val _bookmarkPanelState = MutableStateFlow(BookmarkPanelState())
    val bookmarkPanelState: StateFlow<BookmarkPanelState> =
        _bookmarkPanelState.asStateFlow()

    // ==================== APK ====================

    internal val _apkInfoState = MutableStateFlow(ApkInfoState())
    val apkInfoState: StateFlow<ApkInfoState> = _apkInfoState.asStateFlow()

    internal val _apkBrowserState = MutableStateFlow(ApkBrowserState())
    val apkBrowserState: StateFlow<ApkBrowserState> =
        _apkBrowserState.asStateFlow()

    internal val _pendingInstall = MutableStateFlow<File?>(null)
    val pendingInstall: StateFlow<File?> = _pendingInstall.asStateFlow()

    internal val _installState = MutableStateFlow(InstallState())
    val installState: StateFlow<InstallState> = _installState.asStateFlow()

    // ==================== 已安装应用 ====================

    internal val _installedAppsState = MutableStateFlow(InstalledAppsState())
    val installedAppsState: StateFlow<InstalledAppsState> =
        _installedAppsState.asStateFlow()

    // ==================== 设置 ====================

    internal val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> =
        _settingsState.asStateFlow()

    internal val _settingsVisible = MutableStateFlow(false)
    val settingsVisible: StateFlow<Boolean> =
        _settingsVisible.asStateFlow()

    internal val _helpVisible = MutableStateFlow(false)
    val helpVisible: StateFlow<Boolean> = _helpVisible.asStateFlow()

    // ==================== 提示 ====================

    internal val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    // ==================== FilePanel 弹窗状态 ====================

    internal val _newFilePrompt = MutableStateFlow(false)
    val newFilePrompt: StateFlow<Boolean> = _newFilePrompt.asStateFlow()
    internal val _newFilePanelIsLeft = MutableStateFlow(true)

    internal val _newFolderDialogVisible = MutableStateFlow(false)
    val newFolderDialogVisible: StateFlow<Boolean> =
        _newFolderDialogVisible.asStateFlow()
    internal val _newFolderDialogIsLeft = MutableStateFlow(true)
    val newFolderDialogIsLeft: StateFlow<Boolean> =
        _newFolderDialogIsLeft.asStateFlow()

    internal val _jumpDialogVisible = MutableStateFlow(false)
    val jumpDialogVisible: StateFlow<Boolean> = _jumpDialogVisible.asStateFlow()
    internal val _jumpDialogIsLeft = MutableStateFlow(true)
    val jumpDialogIsLeft: StateFlow<Boolean> = _jumpDialogIsLeft.asStateFlow()

    internal val _sortDialogVisible = MutableStateFlow(false)
    val sortDialogVisible: StateFlow<Boolean> = _sortDialogVisible.asStateFlow()
    internal val _sortDialogIsLeft = MutableStateFlow(true)
    val sortDialogIsLeft: StateFlow<Boolean> = _sortDialogIsLeft.asStateFlow()

    internal val _actionDialogTarget = MutableStateFlow<FileItem?>(null)
    val actionDialogTarget: StateFlow<FileItem?> =
        _actionDialogTarget.asStateFlow()
    internal val _actionDialogIsLeft = MutableStateFlow(true)
    val actionDialogIsLeft: StateFlow<Boolean> =
        _actionDialogIsLeft.asStateFlow()

    internal val _infoDialogTarget = MutableStateFlow<FileItem?>(null)
    val infoDialogTarget: StateFlow<FileItem?> =
        _infoDialogTarget.asStateFlow()

    internal val _renameDialogTarget = MutableStateFlow<FileItem?>(null)
    val renameDialogTarget: StateFlow<FileItem?> =
        _renameDialogTarget.asStateFlow()
    internal val _renameDialogIsLeft = MutableStateFlow(true)
    val renameDialogIsLeft: StateFlow<Boolean> =
        _renameDialogIsLeft.asStateFlow()

    // ==================== Jobs ====================

    internal var folderSizeJob: Job? = null
    internal var searchJob: Job? = null
    internal var installJob: Job? = null
    internal var appsJob: Job? = null
    internal var apkBrowserSourceIsLeft = true

    private val hexLoadChunk = 64 * 1024
    private var hexFile: File? = null
    private var hexLoadedBytes = 0

    internal fun flowOf(isLeft: Boolean) =
        if (isLeft) _leftPanel else _rightPanel

    // ==================== 初始化 ====================

    fun initDefaultDirs() {
        val root = Uri.fromFile(Environment.getExternalStorageDirectory())
        openDirectory(true, root)
        viewModelScope.launch {
            delay(150)
            openDirectory(false, root)
        }
    }

    // ==================== 浏览 ====================

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
                uri.path?.let { p ->
                    val f = File(p)
                    if (f.isDirectory) {
                        BookmarkStore.addRecent(getApplication(), f)
                    }
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
        val filtered = if (showHidden) raw
        else raw.filter { !it.name.startsWith(".") }

        val cmp: Comparator<FileItem> = when (sort) {
            SortMode.NAME_ASC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            SortMode.NAME_DESC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC ->
                compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
            SortMode.SIZE_DESC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenByDescending { it.size }
            SortMode.TIME_ASC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.lastModified }
            SortMode.TIME_DESC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenByDescending { it.lastModified }
            SortMode.TYPE_ASC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.substringAfterLast('.', "").lowercase() }
            SortMode.TYPE_DESC ->
                compareBy<FileItem> { !it.isDirectory }
                    .thenByDescending {
                        it.name.substringAfterLast('.', "").lowercase()
                    }
        }
        return filtered.sortedWith(cmp)
    }

    fun setSortMode(isLeft: Boolean, mode: SortMode) {
        flowOf(isLeft).update {
            it.copy(
                sortMode = mode,
                items = process(it.rawItems, mode, it.showHidden)
            )
        }
    }

    fun setShowHidden(isLeft: Boolean, show: Boolean) {
        flowOf(isLeft).update {
            it.copy(
                showHidden = show,
                items = process(it.rawItems, it.sortMode, show)
            )
        }
    }

    fun refresh(isLeft: Boolean) {
        flowOf(isLeft).value.currentUri?.let { openDirectory(isLeft, it) }
    }

    fun goUp(isLeft: Boolean) {
        val current = flowOf(isLeft).value.currentUri ?: return
        val parent = engine.getParent(current) ?: return
        openDirectory(isLeft, parent)
    }

    // ==================== 基础文件操作 ====================

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

    // ==================== 新建文件 ====================

    fun openNewFilePrompt(isLeft: Boolean) {
        _newFilePanelIsLeft.value = isLeft
        _newFilePrompt.value = true
    }

    fun dismissNewFilePrompt() {
        _newFilePrompt.value = false
    }

    fun createFile(name: String) {
        _newFilePrompt.value = false
        val isLeft = _newFilePanelIsLeft.value
        val dir = flowOf(isLeft).value.currentUri?.path?.let(::File) ?: return
        viewModelScope.launch {
            try {
                val f = File(dir, name)
                if (f.exists()) {
                    _toast.value = "文件已存在"
                    return@launch
                }
                f.parentFile?.mkdirs()
                f.createNewFile()
                _toast.value = "已创建：${f.name}"
                refresh(isLeft)
            } catch (e: Exception) {
                _toast.value = "创建失败：${e.message}"
            }
        }
    }

    // ==================== FilePanel 弹窗控制 ====================

    fun openNewFolderDialog(isLeft: Boolean) {
        _newFolderDialogIsLeft.value = isLeft
        _newFolderDialogVisible.value = true
    }

    fun dismissNewFolderDialog() {
        _newFolderDialogVisible.value = false
    }

    fun openJumpDialog(isLeft: Boolean) {
        _jumpDialogIsLeft.value = isLeft
        _jumpDialogVisible.value = true
    }

    fun dismissJumpDialog() {
        _jumpDialogVisible.value = false
    }

    fun openSortDialog(isLeft: Boolean) {
        _sortDialogIsLeft.value = isLeft
        _sortDialogVisible.value = true
    }

    fun dismissSortDialog() {
        _sortDialogVisible.value = false
    }

    fun openActionDialog(isLeft: Boolean, item: FileItem) {
        _actionDialogIsLeft.value = isLeft
        _actionDialogTarget.value = item
    }

    fun dismissActionDialog() {
        _actionDialogTarget.value = null
    }

    fun openInfoDialog(item: FileItem) {
        _infoDialogTarget.value = item
    }

    fun dismissInfoDialog() {
        _infoDialogTarget.value = null
        resetFolderSize()
    }

    fun openRenameDialog(isLeft: Boolean, item: FileItem) {
        _renameDialogIsLeft.value = isLeft
        _renameDialogTarget.value = item
    }

    fun dismissRenameDialog() {
        _renameDialogTarget.value = null
    }

    // ==================== 多选 ====================

    fun enterSelection(isLeft: Boolean, uriString: String) {
        flowOf(isLeft).update {
            it.copy(selectedUris = setOf(uriString), isSelectionMode = true)
        }
    }

    fun toggleSelection(isLeft: Boolean, uriString: String) {
        flowOf(isLeft).update { state ->
            val set = state.selectedUris.toMutableSet()
            if (!set.add(uriString)) set.remove(uriString)
            state.copy(
                selectedUris = set,
                isSelectionMode = set.isNotEmpty()
            )
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
        val targets = state.items.filter {
            state.selectedUris.contains(it.uriString)
        }
        viewModelScope.launch {
            targets.forEach { engine.delete(it.uri) }
            clearSelection(isLeft)
            refresh(isLeft)
        }
    }

    fun batchCopy(fromLeft: Boolean) {
        val state = flowOf(fromLeft).value
        val target = flowOf(!fromLeft).value.currentUri ?: return
        val targets = state.items.filter {
            state.selectedUris.contains(it.uriString)
        }
        viewModelScope.launch {
            targets.forEach { engine.copy(it.uri, target) }
            clearSelection(fromLeft)
            openDirectory(!fromLeft, target)
        }
    }

    fun batchMove(fromLeft: Boolean) {
        val state = flowOf(fromLeft).value
        val target = flowOf(!fromLeft).value.currentUri ?: return
        val targets = state.items.filter {
            state.selectedUris.contains(it.uriString)
        }
        viewModelScope.launch {
            targets.forEach { engine.move(it.uri, target) }
            clearSelection(fromLeft)
            refresh(fromLeft)
            openDirectory(!fromLeft, target)
        }
    }

    // ==================== 打开文件 ====================

    fun openFile(item: FileItem) {
        val ext = item.name.substringAfterLast('.', "").lowercase()
        when {
            ext == "apk" -> openApkInfo(item)
            com.ertyu.model.ArchiveFormat.isArchive(item.name) ->
                _zipPrompt.value = ZipPrompt(item)
            ext in TEXT_FILE_EXTS -> openInEditor(item)
            ext in IMAGE_FILE_EXTS -> openInImageViewer(item)
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
                    s.copy(
                        content = text,
                        originalContent = text,
                        isLoading = false
                    )
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

    // ==================== HEX 查看器 ====================

    fun openHexViewer(item: FileItem) {
        val file = item.uri.path?.let(::File) ?: run {
            _toast.value = "无效路径"
            return
        }
        if (!file.exists() || !file.isFile) {
            _toast.value = "文件不存在"
            return
        }
        hexFile = file
        hexLoadedBytes = 0
        _hexViewerState.value = HexViewerState(
            visible = true,
            filePath = file.absolutePath,
            fileName = file.name,
            fileSize = file.length(),
            isLoading = true
        )
        loadHexChunk()
    }

    fun loadMoreHex() {
        if (hexFile == null) return
        _hexViewerState.update { it.copy(isLoading = true) }
        loadHexChunk()
    }

    private fun loadHexChunk() {
        val file = hexFile ?: return
        viewModelScope.launch {
            try {
                val chunk = withContext(Dispatchers.IO) {
                    RandomAccessFile(file, "r").use { raf ->
                        val remaining = (file.length() - hexLoadedBytes)
                            .coerceAtMost(hexLoadChunk.toLong())
                        if (remaining <= 0) return@withContext ByteArray(0)
                        raf.seek(hexLoadedBytes.toLong())
                        val buf = ByteArray(remaining.toInt())
                        raf.readFully(buf)
                        buf
                    }
                }

                val oldBytes = _hexViewerState.value.bytes
                val newBytes = oldBytes + chunk
                hexLoadedBytes += chunk.size

                _hexViewerState.update {
                    it.copy(
                        bytes = newBytes,
                        loadedBytes = hexLoadedBytes,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _hexViewerState.update {
                    it.copy(isLoading = false, error = e.message ?: "读取失败")
                }
            }
        }
    }

    fun closeHexViewer() {
        hexFile = null
        hexLoadedBytes = 0
        _hexViewerState.value = HexViewerState()
    }

    // ==================== 文件夹大小 ====================

    fun calculateFolderSize(item: FileItem) {
        folderSizeJob?.cancel()
        val folder = item.uri.path?.let(::File) ?: return
        _folderSizeState.value = FolderSizeState(isCalculating = true)
        folderSizeJob = viewModelScope.launch {
            try {
                val (bytes, count) = FolderSizeCalculator.calculate(folder) {
                        scanned, totalBytes ->
                    _folderSizeState.value = FolderSizeState(
                        isCalculating = true,
                        scanned = scanned,
                        totalBytes = totalBytes
                    )
                }
                _folderSizeState.value = FolderSizeState(
                    isCalculating = false,
                    scanned = count,
                    totalBytes = bytes,
                    done = true
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _folderSizeState.value = FolderSizeState(
                    error = e.message ?: "计算失败"
                )
            }
        }
    }

    fun cancelFolderSize() {
        folderSizeJob?.cancel()
        folderSizeJob = null
        _folderSizeState.value = FolderSizeState()
    }

    fun resetFolderSize() {
        folderSizeJob?.cancel()
        folderSizeJob = null
        _folderSizeState.value = FolderSizeState()
    }

    fun clearToast() {
        _toast.value = null
    }
}