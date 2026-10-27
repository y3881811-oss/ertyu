package com.ertyu

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.FileEngine
import com.ertyu.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

internal val TEXT_FILE_EXTS = setOf("txt", "md", "json", "xml", "kt", "java", "kts", "gradle", "properties", "yml", "yaml", "html", "css", "js", "log", "ini", "conf", "cfg", "sh", "bat")
internal val IMAGE_FILE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

class MainViewModel(app: Application) : AndroidViewModel(app) {

    internal var engine: FileEngine = DirectFileEngine()

    internal val _leftPanel = MutableStateFlow(PanelState())
    val leftPanel: StateFlow<PanelState> = _leftPanel.asStateFlow()
    internal val _rightPanel = MutableStateFlow(PanelState())
    val rightPanel: StateFlow<PanelState> = _rightPanel.asStateFlow()

    internal val _editor = MutableStateFlow(EditorState())
    val editor: StateFlow<EditorState> = _editor.asStateFlow()
    internal val _imageViewer = MutableStateFlow(ImageViewerState())
    val imageViewer: StateFlow<ImageViewerState> = _imageViewer.asStateFlow()
    internal val _hexViewerState = MutableStateFlow(HexViewerState())
    val hexViewerState: StateFlow<HexViewerState> = _hexViewerState.asStateFlow()

    internal val _zipProgress = MutableStateFlow(ZipProgress())
    val zipProgress: StateFlow<ZipProgress> = _zipProgress.asStateFlow()
    internal val _zipPrompt = MutableStateFlow(ZipPrompt())
    val zipPrompt: StateFlow<ZipPrompt> = _zipPrompt.asStateFlow()
    internal val _compressPromptVisible = MutableStateFlow(false)
    val compressPromptVisible: StateFlow<Boolean> = _compressPromptVisible.asStateFlow()
    internal val _compressPanelIsLeft = MutableStateFlow(true)

    internal val _folderSizeState = MutableStateFlow(FolderSizeState())
    val folderSizeState: StateFlow<FolderSizeState> = _folderSizeState.asStateFlow()

    internal val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()
    internal val _bookmarkPanelState = MutableStateFlow(BookmarkPanelState())
    val bookmarkPanelState: StateFlow<BookmarkPanelState> = _bookmarkPanelState.asStateFlow()

    internal val _apkInfoState = MutableStateFlow(ApkInfoState())
    val apkInfoState: StateFlow<ApkInfoState> = _apkInfoState.asStateFlow()
    internal val _apkBrowserState = MutableStateFlow(ApkBrowserState())
    val apkBrowserState: StateFlow<ApkBrowserState> = _apkBrowserState.asStateFlow()
    internal val _pendingInstall = MutableStateFlow<File?>(null)
    val pendingInstall: StateFlow<File?> = _pendingInstall.asStateFlow()
    internal val _installState = MutableStateFlow(InstallState())
    val installState: StateFlow<InstallState> = _installState.asStateFlow()

    internal val _installedAppsState = MutableStateFlow(InstalledAppsState())
    val installedAppsState: StateFlow<InstalledAppsState> = _installedAppsState.asStateFlow()

    internal val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()
    internal val _settingsVisible = MutableStateFlow(false)
    val settingsVisible: StateFlow<Boolean> = _settingsVisible.asStateFlow()
    internal val _helpVisible = MutableStateFlow(false)
    val helpVisible: StateFlow<Boolean> = _helpVisible.asStateFlow()

    internal val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    internal val _newFilePrompt = MutableStateFlow(false)
    val newFilePrompt: StateFlow<Boolean> = _newFilePrompt.asStateFlow()
    internal val _newFilePanelIsLeft = MutableStateFlow(true)

    internal val _newFolderDialogVisible = MutableStateFlow(false)
    val newFolderDialogVisible: StateFlow<Boolean> = _newFolderDialogVisible.asStateFlow()
    internal val _newFolderDialogIsLeft = MutableStateFlow(true)
    val newFolderDialogIsLeft: StateFlow<Boolean> = _newFolderDialogIsLeft.asStateFlow()

    internal val _jumpDialogVisible = MutableStateFlow(false)
    val jumpDialogVisible: StateFlow<Boolean> = _jumpDialogVisible.asStateFlow()
    internal val _jumpDialogIsLeft = MutableStateFlow(true)
    val jumpDialogIsLeft: StateFlow<Boolean> = _jumpDialogIsLeft.asStateFlow()

    internal val _sortDialogVisible = MutableStateFlow(false)
    val sortDialogVisible: StateFlow<Boolean> = _sortDialogVisible.asStateFlow()
    internal val _sortDialogIsLeft = MutableStateFlow(true)
    val sortDialogIsLeft: StateFlow<Boolean> = _sortDialogIsLeft.asStateFlow()

    internal val _actionDialogTarget = MutableStateFlow<FileItem?>(null)
    val actionDialogTarget: StateFlow<FileItem?> = _actionDialogTarget.asStateFlow()
    internal val _actionDialogIsLeft = MutableStateFlow(true)
    val actionDialogIsLeft: StateFlow<Boolean> = _actionDialogIsLeft.asStateFlow()

    internal val _infoDialogTarget = MutableStateFlow<FileItem?>(null)
    val infoDialogTarget: StateFlow<FileItem?> = _infoDialogTarget.asStateFlow()

    internal val _renameDialogTarget = MutableStateFlow<FileItem?>(null)
    val renameDialogTarget: StateFlow<FileItem?> = _renameDialogTarget.asStateFlow()
    internal val _renameDialogIsLeft = MutableStateFlow(true)
    val renameDialogIsLeft: StateFlow<Boolean> = _renameDialogIsLeft.asStateFlow()

    internal val _deleteTarget = MutableStateFlow<FileItem?>(null)
    val deleteTarget: StateFlow<FileItem?> = _deleteTarget.asStateFlow()
    internal val _deleteTargetIsLeft = MutableStateFlow(true)
    val deleteTargetIsLeft: StateFlow<Boolean> = _deleteTargetIsLeft.asStateFlow()

    internal var folderSizeJob: Job? = null
    internal var searchJob: Job? = null
    internal var installJob: Job? = null
    internal var appsJob: Job? = null
    internal var apkBrowserSourceIsLeft = true

    internal fun flowOf(isLeft: Boolean) = if (isLeft) _leftPanel else _rightPanel

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
            it.copy(isLoading = true, error = null, selectedUris = emptySet(), isSelectionMode = false, lastSelectedUri = null)
        }
        viewModelScope.launch {
            try {
                val all = engine.list(uri)
                val parent = engine.getParent(uri)
                flow.update { state ->
                    state.copy(currentUri = uri, rawItems = all, items = process(all, state.sortMode, state.showHidden), isLoading = false, canGoUp = parent != null)
                }
                uri.path?.let { p ->
                    val f = File(p)
                    if (f.isDirectory) com.ertyu.engine.BookmarkStore.addRecent(getApplication(), f)
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun process(raw: List<FileItem>, sort: SortMode, showHidden: Boolean): List<FileItem> {
        val filtered = if (showHidden) raw else raw.filter { !it.name.startsWith(".") }
        val cmp: Comparator<FileItem> = when (sort) {
            SortMode.NAME_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            SortMode.NAME_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.lowercase() }
            SortMode.SIZE_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.size }
            SortMode.SIZE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.size }
            SortMode.TIME_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModified }
            SortMode.TIME_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified }
            SortMode.TYPE_ASC -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.substringAfterLast('.', "").lowercase() }
            SortMode.TYPE_DESC -> compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.name.substringAfterLast('.', "").lowercase() }
        }
        return filtered.sortedWith(cmp)
    }

    fun setSortMode(isLeft: Boolean, mode: SortMode) {
        flowOf(isLeft).update { it.copy(sortMode = mode, items = process(it.rawItems, mode, it.showHidden)) }
    }

    fun setShowHidden(isLeft: Boolean, show: Boolean) {
        flowOf(isLeft).update { it.copy(showHidden = show, items = process(it.rawItems, it.sortMode, show)) }
    }

    fun refresh(isLeft: Boolean) {
        flowOf(isLeft).value.currentUri?.let { openDirectory(isLeft, it) }
    }

    fun goUp(isLeft: Boolean) {
        val current = flowOf(isLeft).value.currentUri ?: return
        val parent = engine.getParent(current) ?: return
        openDirectory(isLeft, parent)
    }

    fun clearToast() { _toast.value = null }
}

// 扩展私有方法
private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    this.value = function(this.value)
}