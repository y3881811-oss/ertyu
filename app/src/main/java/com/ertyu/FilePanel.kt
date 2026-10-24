package com.ertyu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.PanelState
import kotlinx.coroutines.launch
import java.io.File

private val VIDEO_EXTS = setOf(
    "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts"
)

private val AUDIO_EXTS = setOf(
    "mp3", "wav", "flac", "ogg", "m4a", "aac", "wma"
)

private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

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
        snapshotFlow {
            listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
        }.collect { (idx, offset) ->
            if (currentUriStr.isNotEmpty()) {
                FilePanelScrollStore.save(isLeft, currentUriStr, idx, offset)
            }
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
                path = state.currentUri?.path ?: "未选择目录",
                canGoUp = state.canGoUp,
                showHidden = state.showHidden,
                showSwitch = onSwitchPanel != null,
                onSwitchPanel = { onSwitchPanel?.invoke() },
                onOpenDrawer = onOpenDrawer
            )
        }
        HorizontalDivider()

        // 列表区域
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
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.uriString },
                        contentType = { if (it.isDirectory) "dir" else "file" }
                    ) { item ->
                        FileRow(
                            item = item,
                            isSelected = state.selectedUris.contains(item.uriString),
                            isSelectionMode = state.isSelectionMode,
                            onClick = {
                                if (state.isSelectionMode) {
                                    // 使用区间选择逻辑
                                    vm.handleSelectionClick(isLeft, item)
                                } else if (item.isDirectory) {
                                    FilePanelScrollStore.save(isLeft, "", 0, 0)
                                    vm.openDirectory(isLeft, item.uri)
                                } else if (ArchiveFormat.isArchive(item.name)) {
                                    vm.openArchiveViewer(isLeft, item)
                                } else if (item.name.substringAfterLast('.', "").lowercase() in VIDEO_EXTS) {
                                    vm.openVideoPlayer(item)
                                } else if (item.name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS) {
                                    vm.openAudioPlayer(item)
                                } else if (item.name.substringAfterLast('.', "").lowercase() in IMAGE_EXTS) {
                                    vm.openGallery(isLeft, item)
                                } else if (item.name.endsWith(".db") || item.name.endsWith(".sqlite")) {
                                    vm.openSqlite(item)
                                } else if (item.name.endsWith(".ttf") || item.name.endsWith(".otf")) {
                                    vm.openFontPreview(item)
                                } else {
                                    vm.openFile(item)
                                }
                            },
                            onLongClick = {
                                if (!state.isSelectionMode) {
                                    vm.enterSelection(isLeft, item.uriString)
                                }
                            },
                            onMoreClick = { vm.openActionDialog(isLeft, item) }
                        )
                    }
                }
            }

            // 底部悬浮 Dock 栏
            if (!state.isSelectionMode) {
                FileActionDock(
                    isLeft = isLeft,
                    vm = vm,
                    onScrollToTop = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun FileTabBar(
    isLeft: Boolean,
    tabs: List<PanelState>,
    activeIndex: Int,
    currentState: PanelState,
    vm: MainViewModel
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(tabs) { index, tab ->
            val isActive = index == activeIndex
            val displayState = if (isActive) currentState else tab
            val name = displayState.currentUri?.path?.let { File(it).name } ?: "根目录"
            val title = if (name.isBlank()) "/" else name

            Row(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { vm.switchTab(isLeft, index) }
                    .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.size(width = 64.dp, height = 20.dp)
                )
                if (tabs.size > 1) {
                    IconButton(
                        onClick = { vm.closeTab(isLeft, index) },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "关闭",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Box(Modifier.size(20.dp))
                }
            }
        }

        item {
            IconButton(
                onClick = { vm.addTab(isLeft) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建标签",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FileToolbar(
    isLeft: Boolean,
    vm: MainViewModel,
    path: String,
    canGoUp: Boolean,
    showHidden: Boolean,
    showSwitch: Boolean,
    onSwitchPanel: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val settings by vm.settingsState.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Default.Menu, contentDescription = "菜单")
        }
        IconButton(onClick = { vm.goUp(isLeft) }, enabled = canGoUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上级")
        }
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { vm.openJumpDialog(isLeft) }
        )
        if (showSwitch) {
            IconButton(onClick = onSwitchPanel) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "切换")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("搜索") },
                    onClick = { menuExpanded = false; vm.openSearch(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("添加书签") },
                    onClick = { menuExpanded = false; vm.addBookmark(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    onClick = { menuExpanded = false; vm.openNewFolderDialog(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("新建文件") },
                    onClick = { menuExpanded = false; vm.openNewFilePrompt(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    onClick = { menuExpanded = false; vm.openSortDialog(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件") },
                    onClick = { menuExpanded = false; vm.setShowHidden(isLeft, !showHidden) }
                )
                DropdownMenuItem(
                    text = { Text("刷新") },
                    onClick = { menuExpanded = false; vm.refresh(isLeft) }
                )
            }
        }
    }
}