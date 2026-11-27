package com.ertyu

import androidx.compose.material3.MaterialTheme
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.ArchiveFormat
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import com.ertyu.model.DragDropHolder
import com.ertyu.model.HiddenExtHolder
import com.ertyu.model.ItemSizeHolder
import com.ertyu.ui.CompressOptionsDialog
import com.ertyu.ui.ExtractOptionsDialog
import com.ertyu.ui.FileConflictDialog
import com.ertyu.ui.SelectionActionSheet
import kotlinx.coroutines.launch

private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")
private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val SPLIT_APK_EXTS = setOf("apks", "xapk", "apkm")

@OptIn(ExperimentalMaterial3Api::class)
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
    var pullRefreshing by remember { mutableStateOf(false) }
    // 面板重新加载完成后收起下拉刷新指示器
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) pullRefreshing = false
    }

    // 👇 文件夹真实大小：订阅全局结果，并随目录变化重新扫描
    val folderSizes by ItemSizeHolder.sizes.collectAsState()
    val folderSizeEnabled by ItemSizeHolder.enabled.collectAsState()

    // 👇 隐藏指定扩展名
    val hiddenExts by HiddenExtHolder.exts.collectAsState()
    val hiddenExtEnabled by HiddenExtHolder.enabled.collectAsState()
    val shownItems = remember(state.items, hiddenExts, hiddenExtEnabled) {
        state.items.filter { it.isDirectory || !HiddenExtHolder.isHidden(it.name) }
    }
    val hiddenCount = state.items.size - shownItems.size

    // 按大小排序时，目录大小是异步算出来的：算完重新排一次
    LaunchedEffect(folderSizes.size) {
        if (state.sortMode == com.ertyu.model.SortMode.SIZE_ASC ||
            state.sortMode == com.ertyu.model.SortMode.SIZE_DESC
        ) {
            vm.setSortMode(isLeft, state.sortMode)
        }
    }
    LaunchedEffect(state.items, currentUriStr, folderSizeEnabled) {
        vm.startVisibleFolderSizeScan(state.items)
    }

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

    val dragState by DragDropHolder.state.collectAsState()
    val isDropTarget = if (isLeft) dragState.hoverLeft else dragState.hoverRight

    Column(
        modifier = modifier
            .fillMaxHeight()
            .onGloballyPositioned { DragDropHolder.setBounds(isLeft, it.boundsInWindow()) }
            .background(
                if (dragState.active && isDropTarget) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    Color.Transparent
                }
            )
            // 双指左右滑动切换面板（单面板模式下很顺手）
            .pointerInput(onSwitchPanel) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    var twoFingers = false
                    var dx = 0f
                    var lastX = 0f
                    var first = true
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break
                        if (pressed.size >= 2) twoFingers = true
                        val x = pressed.first().position.x
                        if (first) {
                            lastX = x
                            first = false
                        } else {
                            dx += x - lastX
                            lastX = x
                        }
                    }
                    if (twoFingers && kotlin.math.abs(dx) > 90f) onSwitchPanel?.invoke()
                }
            }
    ) {
        
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

        if (hiddenCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { HiddenExtHolder.visible.value = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "已隐藏 $hiddenCount 项（点此管理类型）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        PullToRefreshBox(
            isRefreshing = pullRefreshing,
            onRefresh = {
                pullRefreshing = true
                vm.refresh(isLeft)
            },
            modifier = Modifier.weight(1f)
        ) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("错误: ${state.error}") }
                shownItems.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("这个文件夹是空的", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "可以从另一个面板复制/移动文件过来，或用左下角按钮新建文件夹",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                settings.isGridMode -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp, start = 4.dp, end = 4.dp)
                    ) {
                        gridItems(items = shownItems, key = { it.uriString }, contentType = { "file_grid" }) { item ->
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
                        items(items = shownItems, key = { it.uriString }, contentType = { "file_list" }) { item ->
                            FileRow(
                                item = item,
                                isSelected = { state.selectedUris.contains(item.uriString) },
                                isSelectionMode = state.isSelectionMode,
                                dragFromLeft = isLeft,
                                folderSizeText = if (folderSizeEnabled && item.isDirectory) {
                                    folderSizes[item.path]?.let { ItemSizeEngine.format(it) }
                                } else null,
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
            onExtractArchive = { items -> vm.openExtractOptions(items, isLeft) }, // 触发解压弹窗
            onBatchChecksum = { items -> vm.openBatchChecksum(items) },
            onBatchInstall = { items -> vm.openBatchInstall(items) },
            onViewManifest = { item -> vm.openManifestViewer(item) },
            onSplit = { item -> vm.openSplitDialog(item) },
            onMerge = { item -> vm.openMergeDialog(item) }
        )

        // ================== 挂载新弹窗 ==================
        FileConflictDialog(state = conflictState, onDismiss = { vm.clearConflictState() })
        BatchMkdirDialog(vm)
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