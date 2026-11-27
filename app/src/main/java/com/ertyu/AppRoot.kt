package com.ertyu

import com.ertyu.model.IncomingShareHolder
import com.ertyu.model.ManifestHolder
import com.ertyu.model.WebDavClientHolder
import com.ertyu.model.TaskCenterHolder
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.ertyu.model.BatchChecksumHolder
import com.ertyu.model.BatchInstallHolder
import com.ertyu.model.DirCompareHolder
import com.ertyu.model.HiddenExtHolder
import com.ertyu.model.LanScanHolder
import com.ertyu.model.LogStateHolder
import com.ertyu.model.SftpHolder
import com.ertyu.model.SafeModeHolder
import com.ertyu.model.SpaceAnalyzerHolder

@Composable
fun AppRoot(vm: MainViewModel) {
    val context = LocalContext.current

    // 从其他应用分享进来的文件：等目录加载完再导入
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        val pending = IncomingShareHolder.uris.value
        if (pending.isNotEmpty()) {
            IncomingShareHolder.uris.value = emptyList()
            vm.importDroppedUris(pending, targetIsLeft = true)
        }
    }

    // 上次触发的安装是否真的装上了：没装上就提示一次（安装包还在缓存里）
    LaunchedEffect(Unit) {
        com.ertyu.engine.PendingInstall.takeOutcome(context)?.let { o ->
            if (!o.success) {
                vm.showToast("上次更新未完成（可能被取消了），安装包仍在缓存，可重新下载安装")
            }
        }
    }

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
    val safeModeState by SafeModeHolder.state.collectAsState()

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
    val hiddenExtVisible by HiddenExtHolder.visible.collectAsState()
    val dirCompareState by DirCompareHolder.state.collectAsState()
    val batchChecksumState by BatchChecksumHolder.state.collectAsState()
    val sftpState by SftpHolder.state.collectAsState()
    val batchInstallState by BatchInstallHolder.state.collectAsState()
    val lanScanState by LanScanHolder.state.collectAsState()
    val taskCenterVisible by TaskCenterHolder.visible.collectAsState()
    val manifestState by ManifestHolder.state.collectAsState()
    val webDavClientState by WebDavClientHolder.state.collectAsState()

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

    // 启动后检查是否连续崩溃过；健康运行 20 秒即视为正常启动
    LaunchedEffect(Unit) {
        vm.checkSafeModeOnLaunch()
        vm.runRecycleAutoPurge()
        delay(20_000)
        com.ertyu.engine.CrashGuard.markHealthy(context.applicationContext)
    }

    // ================== 全局弹窗 ==================
    AppDialogs(vm)
    SafeModeDialog(vm)
    DragMoveConfirmDialog(vm)
    CryptoDialog(vm)
    UpdateDownloadDialog(vm)
    SplitDialog(vm)
    CrashPromptDialog(vm)

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
        webDavClientState.visible -> {
            BackHandler { vm.closeWebDavClient() }
            WebDavClientScreen(vm)
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
        
        manifestState.visible -> {
            BackHandler { vm.closeManifestViewer() }
            ManifestViewerScreen(vm)
        }
        taskCenterVisible -> {
            BackHandler { vm.closeTaskCenter() }
            TaskCenterScreen(vm)
        }
        lanScanState.visible -> {
            BackHandler { vm.closeLanScan() }
            LanScanScreen(vm)
        }
        batchInstallState.visible -> {
            BackHandler { vm.closeBatchInstall() }
            BatchInstallScreen(vm)
        }
        sftpState.visible -> {
            BackHandler { vm.closeSftp() }
            SftpScreen(vm)
        }
        batchChecksumState.visible -> {
            BackHandler { vm.closeBatchChecksum() }
            BatchChecksumScreen(vm)
        }
        dirCompareState.visible -> {
            BackHandler { vm.closeDirCompare() }
            DirCompareScreen(vm)
        }
        hiddenExtVisible -> {
            HiddenExtScreen(vm)
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