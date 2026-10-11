package com.ertyu

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ApkBrowser
import com.ertyu.engine.ApkInspector
import com.ertyu.engine.ApkInstaller
import com.ertyu.engine.ArchiveEngine
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.FileEngine
import com.ertyu.engine.FolderSizeCalculator
import com.ertyu.engine.SearchEngine
import com.ertyu.model.ApkBrowserState
import com.ertyu.model.ApkEntry
import com.ertyu.model.ApkInfo
import com.ertyu.model.ApkInfoState
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.EditorState
import com.ertyu.model.FileItem
import com.ertyu.model.FolderSizeState
import com.ertyu.model.ImageViewerState
import com.ertyu.model.InstallState
import com.ertyu.model.PanelState
import com.ertyu.model.SearchState
import com.ertyu.model.SortMode
import com.ertyu.model.ZipProgress
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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

    private val _compressPromptVisible = MutableStateFlow(false)
    val compressPromptVisible: StateFlow<Boolean> = _compressPromptVisible.asStateFlow()

    private val _compressPanelIsLeft = MutableStateFlow(true)

    private val _folderSizeState = MutableStateFlow(FolderSizeState())
    val folderSizeState: StateFlow<FolderSizeState> = _folderSizeState.asStateFlow()

    private val _searchState = MutableStateFlow(SearchState())
    val searchState: StateFlow<SearchState> = _searchState.asStateFlow()

    private val _apkInfoState = MutableStateFlow(ApkInfoState())
    val apkInfoState: StateFlow<ApkInfoState> = _apkInfoState.asStateFlow()

    private val _apkBrowserState = MutableStateFlow(ApkBrowserState())
    val apkBrowserState: StateFlow<ApkBrowserState> = _apkBrowserState.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _pendingInstall = MutableStateFlow<File?>(null)
    val pendingInstall: StateFlow<File?> = _pendingInstall.asStateFlow()

    private val _installState = MutableStateFlow(InstallState())
    val installState: StateFlow<InstallState> = _installState.asStateFlow()

    private var folderSizeJob: Job? = null
    private var searchJob: Job? = null
    private var installJob: Job? = null

    private var apkBrowserSourceIsLeft = true

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

    fun openFile(item: FileItem) {
        val ext = item.name.substringAfterLast('.', "").lowercase()
        when {
            ext == "apk" -> openApkInfo(item)
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

    // ================= APK 浏览 =================

    fun openApkBrowser(isLeft: Boolean, item: FileItem) {
        val apkFile = item.uri.path?.let(::File) ?: run {
            _toast.value = "无效路径"
            return
        }
        apkBrowserSourceIsLeft = isLeft
        _apkBrowserState.value = ApkBrowserState(
            visible = true,
            apkPath = apkFile.absolutePath,
            apkName = apkFile.name,
            isLoading = true
        )
        viewModelScope.launch {
            val result = ApkBrowser.list(apkFile)
            result.onSuccess { entries ->
                _apkBrowserState.update {
                    it.copy(isLoading = false, entries = entries)
                }
            }.onFailure { e ->
                _apkBrowserState.update {
                    it.copy(isLoading = false, error = e.message ?: "读取失败")
                }
            }
        }
    }

    fun closeApkBrowser() {
        _apkBrowserState.value = ApkBrowserState()
    }

    fun updateApkBrowserFilter(q: String) {
        _apkBrowserState.update { it.copy(filterQuery = q) }
    }

    fun extractApkEntry(entry: ApkEntry) {
        val apkPath = _apkBrowserState.value.apkPath
        val apkFile = File(apkPath)
        val targetDir = flowOf(apkBrowserSourceIsLeft).value.currentUri?.path?.let(::File)
            ?: run {
                _toast.value = "无目标目录"
                return
            }
        viewModelScope.launch {
            val result = ApkBrowser.extractEntry(apkFile, entry.path, targetDir)
            result.onSuccess { out ->
                _toast.value = "已提取：${out.name}"
                refresh(apkBrowserSourceIsLeft)
            }.onFailure { e ->
                _toast.value = "提取失败：${e.message}"
            }
        }
    }

    // ================= APK 安装 =================

    fun installApk(item: FileItem) {
        val apkFile = item.uri.path?.let(::File) ?: run {
            _toast.value = "无效的 APK 路径"
            return
        }
        startInstall(apkFile)
    }

    fun installApkFromInfo(info: ApkInfo) {
        startInstall(File(info.filePath))
    }

    private fun startInstall(apkFile: File) {
        installJob?.cancel()
        _installState.value = InstallState(
            visible = true,
            fileName = apkFile.name,
            stage = "准备中…"
        )
        installJob = viewModelScope.launch {
            val result = ApkInstaller.install(
                context = getApplication(),
                apkFile = apkFile
            ) { copied, total ->
                _installState.update {
                    it.copy(
                        stage = if (total > 0 && copied >= total)
                            "提交安装…" else "复制中…",
                        copied = copied,
                        total = total
                    )
                }
            }

            when (result) {
                is ApkInstaller.Result.Success -> {
                    _installState.update {
                        it.copy(
                            stage = "已提交，请在弹出的系统界面确认",
                            copied = it.total
                        )
                    }
                    // 延迟自动关闭进度框，让用户看到结果
                    delay(1200)
                    _installState.value = InstallState()
                }
                is ApkInstaller.Result.NeedInstallPermission -> {
                    _installState.value = InstallState()
                    _pendingInstall.value = apkFile
                    _toast.value = "请先允许本应用安装未知应用"
                }
                is ApkInstaller.Result.Failure -> {
                    _installState.update {
                        it.copy(stage = "失败", error = result.message)
                    }
                }
            }
        }
    }

    fun dismissInstallProgress() {
        installJob?.cancel()
        installJob = null
        _installState.value = InstallState()
    }

    fun dismissInstallPermissionPrompt() {
        _pendingInstall.value = null
    }

    // ================= APK 信息 =================

    private fun openApkInfo(item: FileItem) {
        _apkInfoState.value = ApkInfoState(
            visible = true,
            isLoading = true,
            fileName = item.name
        )
        val apkFile = item.uri.path?.let(::File)
        if (apkFile == null) {
            _apkInfoState.value = ApkInfoState(
                visible = true,
                isLoading = false,
                fileName = item.name,
                error = "无效路径"
            )
            return
        }
        viewModelScope.launch {
            val result = ApkInspector.inspect(getApplication(), apkFile)
            result.onSuccess { info ->
                _apkInfoState.update {
                    it.copy(isLoading = false, info = info)
                }
            }.onFailure { e ->
                _apkInfoState.update {
                    it.copy(isLoading = false, error = e.message ?: "解析失败")
                }
            }
        }
    }

    fun closeApkInfo() {
        _apkInfoState.value = ApkInfoState()
    }

    fun saveApkIcon(info: ApkInfo) {
        viewModelScope.launch {
            try {
                val icon = info.icon ?: run {
                    _toast.value = "无图标可保存"
                    return@launch
                }
                val parentDir = File(info.filePath).parentFile ?: run {
                    _toast.value = "无法确定目录"
                    return@launch
                }
                val safeLabel = info.label.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                val outFile = File(parentDir, "${safeLabel}_${info.packageName}.png")
                FileOutputStream(outFile).use { out ->
                    icon.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                _toast.value = "图标已保存：${outFile.name}"
            } catch (e: Exception) {
                _toast.value = "保存失败：${e.message}"
            }
        }
    }

    fun clearToast() {
        _toast.value = null
    }

    // ================= 解压 =================

    fun dismissZipPrompt() {
        _zipPrompt.value = ZipPrompt()
    }

    fun extractZipHere(isLeft: Boolean, item: FileItem) {
        _zipPrompt.value = ZipPrompt()
        val currentDir = flowOf(isLeft).value.currentUri?.path?.let { File(it) } ?: return
        val archiveFile = item.uri.path?.let { File(it) } ?: return
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

    fun requestCompress(isLeft: Boolean) {
        val state = flowOf(isLeft).value
        if (state.selectedUris.isEmpty()) return
        _compressPanelIsLeft.value = isLeft
        _compressPromptVisible.value = true
    }

    fun dismissCompressPrompt() {
        _compressPromptVisible.value = false
    }

    fun compressWithFormat(format: ArchiveFormat) {
        _compressPromptVisible.value = false
        if (!format.canCompress) return
        val isLeft = _compressPanelIsLeft.value
        val state = flowOf(isLeft).value
        val currentDir = state.currentUri?.path?.let { File(it) } ?: return
        val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
        if (targets.isEmpty()) return

        val sources = targets.mapNotNull { it.uri.path?.let(::File) }
        if (sources.isEmpty()) return

        val ext = when (format) {
            ArchiveFormat.ZIP -> ".zip"
            ArchiveFormat.TAR -> ".tar"
            ArchiveFormat.TAR_GZ -> ".tar.gz"
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
            _zipProgress.value = ZipProgress(
                visible = true,
                title = "压缩中",
                message = fileName
            )
            val result = ArchiveEngine.compress(sources, targetFile, format) { cur, total ->
                _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
            }
            result.onSuccess {
                _zipProgress.value = _zipProgress.value.copy(
                    done = true,
                    message = "压缩完成：$fileName"
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

    // ================= 文件夹大小 =================

    fun calculateFolderSize(item: FileItem) {
        folderSizeJob?.cancel()
        val folder = item.uri.path?.let { File(it) } ?: return
        _folderSizeState.value = FolderSizeState(isCalculating = true)
        folderSizeJob = viewModelScope.launch {
            try {
                val (bytes, count) = FolderSizeCalculator.calculate(folder) { scanned, totalBytes ->
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

    // ================= 搜索 =================

    fun openSearch(isLeft: Boolean) {
        val rootPath = flowOf(isLeft).value.currentUri?.path ?: return
        _searchState.value = SearchState(
            visible = true,
            sourceIsLeft = isLeft,
            rootPath = rootPath
        )
    }

    fun closeSearch() {
        searchJob?.cancel()
        searchJob = null
        _searchState.value = SearchState()
    }

    fun updateSearchQuery(q: String) {
        _searchState.update { it.copy(query = q) }
    }

    fun startSearch() {
        val state = _searchState.value
        if (state.query.isBlank()) return
        searchJob?.cancel()
        _searchState.update {
            it.copy(
                isSearching = true,
                results = emptyList(),
                scannedDirs = 0,
                scannedFiles = 0,
                error = null,
                truncated = false
            )
        }

        searchJob = viewModelScope.launch {
            try {
                val root = File(state.rootPath)
                val (results, truncated) = SearchEngine.search(root, state.query) { dirs, files, list ->
                    _searchState.update {
                        it.copy(scannedDirs = dirs, scannedFiles = files, results = list)
                    }
                }
                _searchState.update {
                    it.copy(isSearching = false, results = results, truncated = truncated)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _searchState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
        _searchState.update { it.copy(isSearching = false) }
    }

    fun handleSearchResultClick(item: FileItem) {
        val isLeft = _searchState.value.sourceIsLeft
        if (item.isDirectory) {
            val uri = item.uri
            closeSearch()
            openDirectory(isLeft, uri)
        } else {
            openFile(item)
        }
    }

    fun revealInPanel(item: FileItem) {
        val isLeft = _searchState.value.sourceIsLeft
        val parent = item.uri.path?.let { File(it).parentFile } ?: return
        if (!parent.exists()) return
        closeSearch()
        openDirectory(isLeft, Uri.fromFile(parent))
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