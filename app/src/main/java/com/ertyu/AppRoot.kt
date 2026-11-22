package com.ertyu

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.ertyu.model.BatchChecksumHolder
import com.ertyu.model.DirCompareHolder
import com.ertyu.model.LogStateHolder
import com.ertyu.model.SpaceAnalyzerHolder

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current

    // ================== 状态收集 ==================
    val editor by vm.editor.collectAsState()
    val imageViewer by vm.imageViewer.collectAsState()
    val hexViewerState by vm.hexViewerState.collectAsState()
    val searchState by vm.searchState.collectAsState()
    val apkState by vm.apkInfoState.collectAsState()
    val apkBrowserState by vm.apkBrowserState.collectAsState()
    val bookmarkState by vm.bookmarkPanelState.collectAsState()
    val installedAppsState by vm.installedAppsState.collectAsState()
    val settingsVisible by vm.settingsVisible.collectAsState()
    val helpVisible by vm.helpVisible.collectAsState()

    // 新增的关于界面状态
    val aboutVisible by _aboutVisible.collectAsState()
    // 新增的日志界面状态
    val logState by LogStateHolder.state.collectAsState()

    // 各种 Holder 状态
    val signerState by ApkToolsState.signerState.collectAsState()
    val dexViewerState by ApkToolsState.dexViewerState.collectAsState()
    val diffState by DiffStateHolder.state.collectAsState()
    val webDavState by WebDavStateHolder.state.collectAsState()
    val archiveViewerState by ArchiveViewerStateHolder.state.collectAsState()
    val videoPlayerState by VideoPlayerStateHolder.state.collectAsState()
    val audioPlayerState by AudioPlayerStateHolder.state.collectAsState()
    val galleryState by GalleryStateHolder.state.collectAsState()
    val sqliteState by SqliteStateHolder.state.collectAsState()
    val networkState by NetworkShareStateHolder.state.collectAsState()
    val fontState by FontPreviewStateHolder.state.collectAsState()
    val recycleBinState by RecycleBinStateHolder.state.collectAsState()
    val spaceAnalyzerState by SpaceAnalyzerHolder.state.collectAsState()
    val dirCompareState by DirCompareHolder.state.collectAsState()
    val batchChecksumState by BatchChecksumHolder.state.collectAsState()

    val terminalState by TerminalStateHolder.state.collectAsState()
    val processState by TerminalStateHolder.processState.collectAsState()

    // 高级功能状态
    val contentSearchState by vm.contentSearchState.collectAsState()
    val duplicateScanState by vm.duplicateScanState.collectAsState()

    // ================== SAF 授权逻辑 ==================
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                vm.openDirectory(true, uri)
                vm.showToast("授权成功")
            } catch (e: Exception) {
                vm.showToast("获取权限失败: ${e.message}")
            }
        } else {
            vm.showToast("已取消授权")
        }
    }

    val needSafAuth by _termuxSafRequestState.collectAsState()
    LaunchedEffect(needSafAuth) {
        if (needSafAuth) {
            vm.dismissTermuxSafRequest()
            safLauncher.launch(null)
        }
    }

    // ================== 全局弹窗 ==================
    AppDialogs(vm)
    CryptoDialog(vm)

    // ================== 页面路由 ==================
    when {
        // --- 优先级最高的全屏工具类 ---
        editor.isOpen -> {
            BackHandler { vm.closeEditor() }
            EditorScreen(vm)
        }
        imageViewer.isOpen -> {
            BackHandler { vm.closeImageViewer() }
            ImageViewerScreen(vm)
        }
        contentSearchState.isSearching || contentSearchState.results.isNotEmpty() -> {
            BackHandler { vm.closeContentSearch() }
            ContentSearchScreen(vm, java.io.File("/storage/emulated/0"))
        }
        duplicateScanState.isScanning || duplicateScanState.groups.isNotEmpty() -> {
            BackHandler { vm.closeDuplicateScan() }
            DuplicateScanScreen(vm, java.io.File("/storage/emulated/0"))
        }
        galleryState.visible -> {
            GalleryScreen(vm)
        }
        hexViewerState.visible -> {
            BackHandler { vm.closeHexViewer() }
            HexViewerScreen(vm)
        }
        sqliteState.visible -> {
            SqliteViewerScreen(vm)
        }
        fontState.visible -> {
            FontPreviewScreen(vm)
        }
        signerState.visible -> {
            BackHandler { vm.closeApkSigner() }
            ApkSignerScreen(vm)
        }
        dexViewerState.visible -> {
            BackHandler {
                if (dexViewerState.selectedClass != null) {
                    vm.closeSmaliClass()
                } else {
                    vm.closeDexViewer()
                }
            }
            DexViewerScreen(vm)
        }
        diffState.visible -> {
            BackHandler { vm.closeDiff() }
            DiffScreen(vm)
        }
        archiveViewerState.visible -> {
            BackHandler { vm.closeArchiveViewer() }
            ArchiveViewerScreen(vm)
        }
        videoPlayerState.visible -> {
            VideoPlayerScreen(vm)
        }
        audioPlayerState.visible -> {
            AudioPlayerScreen(vm)
        }
        webDavState.isRunning || webDavState.sharePath.isNotEmpty() -> {
            BackHandler {
                if (webDavState.isRunning) vm.stopWebDav()
                vm.closeWebDav()
            }
            WebDavScreen(vm)
        }
        networkState.visible -> {
            BackHandler { vm.closeNetworkShare() }
            NetworkShareScreen(vm)
        }
        searchState.visible -> {
            BackHandler { vm.closeSearch() }
            SearchScreen(vm)
        }
        apkState.visible -> {
            BackHandler { vm.closeApkInfo() }
            ApkInfoScreen(vm)
        }
        apkBrowserState.visible -> {
            BackHandler { vm.closeApkBrowser() }
            ApkBrowserScreen(vm)
        }
        bookmarkState.visible -> {
            BackHandler { vm.closeBookmarkPanel() }
            BookmarkScreen(vm)
        }
        installedAppsState.visible -> {
            BackHandler { vm.closeInstalledApps() }
            InstalledAppsScreen(vm)
        }
        
        // --- 新增：日志查看器 ---
        logState.visible -> {
            BackHandler { vm.closeLogViewer() }
            LogViewerScreen(vm)
        }
        
        batchChecksumState.visible -> {
            BackHandler { vm.closeBatchChecksum() }
            BatchChecksumScreen(vm)
        }
        dirCompareState.visible -> {
            BackHandler { vm.closeDirCompare() }
            DirCompareScreen(vm)
        }
        spaceAnalyzerState.visible -> {
            BackHandler { vm.closeSpaceAnalyzer() }
            SpaceAnalyzerScreen(vm)
        }
        recycleBinState.visible -> {
            BackHandler { vm.closeRecycleBin() }
            RecycleBinScreen(vm)
        }
        terminalState.visible -> {
            BackHandler { vm.closeTerminal() }
            TerminalScreen(vm)
        }
        processState.visible -> {
            BackHandler { vm.closeProcessManager() }
            ProcessManagerScreen(vm)
        }
        settingsVisible -> {
            BackHandler { vm.closeSettings() }
            SettingsScreen(vm)
        }
        helpVisible -> {
            BackHandler { vm.closeHelp() }
            HelpScreen(vm)
        }
        
        // --- 新增：关于软件 ---
        aboutVisible -> {
            BackHandler { vm.closeAbout() }
            AboutScreen(vm)
        }
        
        // --- 默认：主文件浏览界面 ---
        else -> {
            MainScreen(vm)
        }
    }
}