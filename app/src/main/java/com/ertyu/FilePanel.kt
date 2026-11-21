package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat
import com.ertyu.ui.CompressOptionsDialog
import com.ertyu.ui.ExtractOptionsDialog
import com.ertyu.ui.FileConflictDialog
import com.ertyu.ui.SelectionActionSheet
import kotlinx.coroutines.launch

private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")
private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val SPLIT_APK_EXTS = setOf("apks", "xapk", "apkm")

@Composable
fun FilePanel(
    isLeft: Boolean,
    isCompact: Boolean = false,
    vm: MainViewModel,
    onSwitchPanel: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()
    val settings by vm.settingsState.collectAsState()
    val conflictState by vm.fileConflictState.collectAsState()
    val clipboard by vm.fileClipboardState.collectAsState()

    val currentUriStr = state.currentUri?.toString() ?: ""
    val savedUri = FilePanelScrollStore.savedUri(isLeft)
    val isSameDir = currentUriStr.isNotEmpty() && currentUriStr == savedUri
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isSameDir) FilePanelScrollStore.savedIndex(isLeft) else 0,
        initialFirstVisibleItemScrollOffset = if (isSameDir) FilePanelScrollStore.savedOffset(isLeft) else 0
    )
    val coroutineScope = rememberCoroutineScope()

    var showSelectionSheet by remember { mutableStateOf(false) }

    // 返回键拦截：优先关闭底部弹窗 -> 其次退出多选
    BackHandler(enabled = showSelectionSheet || state.isSelectionMode) {
        if (showSelectionSheet) {
            showSelectionSheet = false
        } else if (state.isSelectionMode) {
            vm.clearSelection(isLeft)
        }
    }

    LaunchedEffect(listState, currentUriStr) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (idx, offset) ->
                if (currentUriStr.isNotEmpty()) FilePanelScrollStore.save(isLeft, currentUriStr, idx, offset)
            }
    }

    Column(modifier = modifier.fillMaxHeight()) {
        
        // 顶部路径栏
        FileToolbar(
            isLeft = isLeft,
            isCompact = isCompact,
            vm = vm,
            isGridMode = settings.isGridMode,
            onToggleGrid = { vm.toggleGridMode() },
            path = state.currentUri?.path ?: "未选择目录",
            canGoUp = state.canGoUp,
            showHidden = state.showHidden,
            showSwitch = onSwitchPanel != null,
            onSwitchPanel = { onSwitchPanel?.invoke() },
            onOpenDrawer = onOpenDrawer
        )
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("错误: ${state.error}") }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("空目录") }
                settings.isGridMode -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp, start = 4.dp, end = 4.dp)
                    ) {
                        gridItems(items = state.items, key = { it.uriString }, contentType = { "file_grid" }) { item ->
                            FileGridItem(
                                item = item,
                                isSelected = { state.selectedUris.contains(item.uriString) },
                                onClick = { handleItemClick(isLeft, item, state, vm) },
                                onLongClick = {
                                    if (!state.isSelectionMode) {
                                        vm.enterSelection(isLeft, item.uriString)
                                    } else if (item.uriString in state.selectedUris) {
                                        showSelectionSheet = true
                                    } else {
                                        vm.toggleSelection(isLeft, item.uriString)
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                    ) {
                        items(items = state.items, key = { it.uriString }, contentType = { "file_list" }) { item ->
                            FileRow(
                                item = item,
                                isSelected = { state.selectedUris.contains(item.uriString) },
                                isSelectionMode = state.isSelectionMode,
                                onClick = { handleItemClick(isLeft, item, state, vm) },
                                onLongClick = {
                                    if (!state.isSelectionMode) {
                                        vm.enterSelection(isLeft, item.uriString)
                                    } else if (item.uriString in state.selectedUris) {
                                        showSelectionSheet = true
                                    } else {
                                        vm.toggleSelection(isLeft, item.uriString)
                                    }
                                },
                                onMoreClick = { vm.openActionDialog(isLeft, item) }
                            )
                        }
                    }
                }
            }

            if (!isCompact && !state.isSelectionMode) {
                FileActionDock(
                    isLeft = isLeft,
                    vm = vm,
                    onScrollToTop = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // ================== 底部多选菜单 ==================
        val selectedItems = state.items.filter { it.uriString in state.selectedUris }
        SelectionActionSheet(
            visible = showSelectionSheet,
            selectedItems = selectedItems,
            defaultUseTrash = settings.useRecycleBin,
            hasClipboardItems = !clipboard.isEmpty,
            onDismiss = { showSelectionSheet = false },
            onSelectAll = { vm.selectAll(isLeft) },
            onInvertSelection = { vm.invertSelection(isLeft) },
            onCopyToClipboard = { vm.copyToClipboard(isLeft) },
            onCutToClipboard = { vm.cutToClipboard(isLeft) },
            onPaste = { vm.pasteFromClipboard(isLeft) },
            onCopyToOther = { vm.batchCopy(isLeft) },
            onMoveToOther = { vm.batchMove(isLeft) },
            onDelete = { useTrash -> vm.batchDelete(isLeft, useTrash) },
            onCompress = { items -> vm.openCompressOptions(items, isLeft) },     // 触发压缩弹窗
            onBatchRename = { vm.openBatchRename(isLeft) },
            onInstallApk = { item -> vm.installApk(item) },
            onBrowseApk = { item -> vm.openApkBrowser(isLeft, item) },
            onExtractArchive = { items -> vm.openExtractOptions(items, isLeft) } // 触发解压弹窗
        )

        // ================== 挂载新弹窗 ==================
        FileConflictDialog(state = conflictState, onDismiss = { vm.clearConflictState() })
        ExtractOptionsDialog(vm)
        CompressOptionsDialog(vm)
    }
}

private fun handleItemClick(
    isLeft: Boolean,
    item: com.ertyu.model.FileItem,
    state: com.ertyu.model.PanelState,
    vm: MainViewModel
) {
    if (state.isSelectionMode) {
        vm.handleSelectionClick(isLeft, item)
        return
    }
    if (item.isDirectory) {
        FilePanelScrollStore.save(isLeft, "", 0, 0)
        vm.openDirectory(isLeft, item.uri)
        return
    }
    val ext = item.name.substringAfterLast('.', "").lowercase()
    when {
        ext in SPLIT_APK_EXTS -> vm.installApk(item)
        ext == "apk" -> vm.openFile(item)
        ArchiveFormat.isArchive(item.name) -> vm.openArchiveViewer(isLeft, item)
        ext in VIDEO_EXTS -> vm.openVideoPlayer(item)
        ext in AUDIO_EXTS -> vm.openAudioPlayer(item)
        ext in IMAGE_EXTS -> vm.openGallery(isLeft, item)
        ext == "db" || ext == "sqlite" -> vm.openSqlite(item)
        ext == "ttf" || ext == "otf" -> vm.openFontPreview(item)
        else -> vm.openFile(item)
    }
}