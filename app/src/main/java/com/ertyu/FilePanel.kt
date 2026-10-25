package com.ertyu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat
import kotlinx.coroutines.launch
import java.io.File

private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")
private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val SPLIT_APK_EXTS = setOf("apks", "xapk", "apkm")

@Composable
fun FilePanel(
    isLeft: Boolean,
    vm: MainViewModel,
    onSwitchPanel: (() -> Unit)? = null,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()
    val settings by vm.settingsState.collectAsState()

    val tabsFlow = if (isLeft) TabManager.leftTabList else TabManager.rightTabList
    val activeFlow = if (isLeft) TabManager.leftActiveIndex else TabManager.rightActiveIndex
    val tabs by tabsFlow.collectAsState()
    val activeIndex by activeFlow.collectAsState()

    val currentUriStr = state.currentUri?.toString() ?: ""
    val savedUri = FilePanelScrollStore.savedUri(isLeft)
    val isSameDir = currentUriStr.isNotEmpty() && currentUriStr == savedUri

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isSameDir) FilePanelScrollStore.savedIndex(isLeft) else 0,
        initialFirstVisibleItemScrollOffset = if (isSameDir) FilePanelScrollStore.savedOffset(isLeft) else 0
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(listState, currentUriStr) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (idx, offset) ->
                if (currentUriStr.isNotEmpty()) FilePanelScrollStore.save(isLeft, currentUriStr, idx, offset)
            }
    }

    Column(modifier = modifier.fillMaxHeight()) {
        FileTabBar(
            isLeft = isLeft,
            tabs = tabs,
            activeIndex = activeIndex,
            currentState = state,
            vm = vm
        )

        if (state.isSelectionMode) {
            SelectionToolbar(
                count = state.selectedUris.size,
                onClear = { vm.clearSelection(isLeft) },
                onSelectAll = { vm.selectAll(isLeft) },
                onInvert = { vm.invertSelection(isLeft) },
                onSelectByType = { type -> vm.selectByType(isLeft, type) },
                onRename = { vm.openBatchRename(isLeft) },
                onZip = { vm.requestCompress(isLeft) },
                onCopy = { vm.batchCopy(isLeft) },
                onMove = { vm.batchMove(isLeft) },
                onDelete = { vm.batchDelete(isLeft) },
                onDiff = { vm.startFileDiff(isLeft) }
            )
        } else {
            FileToolbar(
                isLeft = isLeft,
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
        }
        HorizontalDivider()

        Box(modifier = Modifier.weight(1f)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("错误: ${state.error}")
                }
                state.items.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(if (state.showHidden) "空目录" else "空目录（可尝试显示隐藏文件）")
                }
                settings.isGridMode -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 96.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp, start = 4.dp, end = 4.dp)
                    ) {
                        gridItems(items = state.items, key = { it.uriString }) { item ->
                            FileGridItem(
                                item = item,
                                isSelected = state.selectedUris.contains(item.uriString),
                                onClick = { handleItemClick(isLeft, item, state, vm) },
                                onLongClick = {
                                    if (!state.isSelectionMode) vm.enterSelection(isLeft, item.uriString)
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
                        items(items = state.items, key = { it.uriString }) { item ->
                            FileRow(
                                item = item,
                                isSelected = state.selectedUris.contains(item.uriString),
                                isSelectionMode = state.isSelectionMode,
                                onClick = { handleItemClick(isLeft, item, state, vm) },
                                onLongClick = {
                                    if (!state.isSelectionMode) vm.enterSelection(isLeft, item.uriString)
                                },
                                onMoreClick = { vm.openActionDialog(isLeft, item) }
                            )
                        }
                    }
                }
            }

            if (!state.isSelectionMode) {
                FileActionDock(
                    isLeft = isLeft,
                    vm = vm,
                    onScrollToTop = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * 统一的点击处理逻辑，抽离出来方便维护
 */
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
        // 新增：拆分 APK 直接安装
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