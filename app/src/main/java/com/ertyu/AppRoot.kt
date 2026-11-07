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
import java.io.File

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current

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
    val logState by LogStateHolder.state.collectAsState()
    val recycleBinState by RecycleBinStateHolder.state.collectAsState()

    val terminalState by TerminalStateHolder.state.collectAsState()
    val processState by TerminalStateHolder.processState.collectAsState()

    // 新增功能的状态
    val contentSearchState by vm.contentSearchState.collectAsState()
    val duplicateScanState by vm.duplicateScanState.collectAsState()

    // ================== SAF 文件夹授权逻辑 ==================
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
    // ==========================================================

    AppDialogs(vm)
    CryptoDialog(vm)

    when {
        editor.isOpen -> {
            BackHandler { vm.closeEditor() }
            EditorScreen(vm)
        }
        imageViewer.isOpen -> {
            BackHandler { vm.closeImageViewer() }
            ImageViewerScreen(vm)
        }
        // 👇 挂载全局内容搜索界面
        contentSearchState.isSearching || contentSearchState.results.isNotEmpty() -> {
            BackHandler { vm.closeContentSearch() }
            ContentSearchScreen(vm, File("/storage/emulated/0"))
        }
        // 👇 挂载重复文件扫描界面
        duplicateScanState.isScanning || duplicateScanState.groups.isNotEmpty() -> {
            BackHandler { vm.closeDuplicateScan() }
            DuplicateScanScreen(vm, File("/storage/emulated/0"))
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
        logState.visible -> {
            BackHandler { vm.closeLogViewer() }
            LogViewerScreen(vm)
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
        else -> {
            MainScreen(vm)
        }
    }
}