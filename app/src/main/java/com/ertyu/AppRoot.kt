package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun AppRoot(vm: MainViewModel) {
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
    
    // 新增：局域网共享状态
    val networkState by NetworkShareStateHolder.state.collectAsState()

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
        // 新增：局域网共享路由
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