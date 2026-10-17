package com.ertyu

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat

@Composable
fun FilePanel(
    isLeft: Boolean,
    vm: MainViewModel,
    onSwitchPanel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()

    val currentUriStr = state.currentUri?.toString() ?: ""
    val savedUri = FilePanelScrollStore.savedUri(isLeft)
    val isSameDir = currentUriStr.isNotEmpty() && currentUriStr == savedUri

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = if (isSameDir) {
            FilePanelScrollStore.savedIndex(isLeft)
        } else 0,
        initialFirstVisibleItemScrollOffset = if (isSameDir) {
            FilePanelScrollStore.savedOffset(isLeft)
        } else 0
    )

    LaunchedEffect(listState, currentUriStr) {
        snapshotFlow {
            listState.firstVisibleItemIndex to
                    listState.firstVisibleItemScrollOffset
        }.collect { (idx, offset) ->
            if (currentUriStr.isNotEmpty()) {
                FilePanelScrollStore.save(isLeft, currentUriStr, idx, offset)
            }
        }
    }

    Column(modifier = modifier.fillMaxHeight()) {
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
                onSwitchPanel = { onSwitchPanel?.invoke() }
            )
        }
        HorizontalDivider()

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("错误: ${state.error}")
            }
            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (state.showHidden) "空目录"
                    else "空目录（可尝试显示隐藏文件）"
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
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
                                vm.toggleSelection(isLeft, item.uriString)
                            } else if (item.isDirectory) {
                                FilePanelScrollStore.save(isLeft, "", 0, 0)
                                vm.openDirectory(isLeft, item.uri)
                            } else if (ArchiveFormat.isArchive(item.name)) {
                                // 点压缩包：进入内部浏览界面
                                vm.openArchiveViewer(isLeft, item)
                            } else {
                                vm.openFile(item)
                            }
                        },
                        onLongClick = {
                            if (!state.isSelectionMode) {
                                vm.openActionDialog(isLeft, item)
                            }
                        },
                        onMoreClick = { vm.openActionDialog(isLeft, item) }
                    )
                }
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
    onSwitchPanel: () -> Unit
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
                Icon(Icons.Default.MoreVert, contentDescription = "菜单")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("搜索") },
                    onClick = {
                        menuExpanded = false
                        vm.openSearch(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("书签") },
                    onClick = {
                        menuExpanded = false
                        vm.openBookmarkPanel(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("添加到书签") },
                    onClick = {
                        menuExpanded = false
                        vm.addBookmark(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("已安装应用") },
                    onClick = {
                        menuExpanded = false
                        vm.openInstalledApps()
                    }
                )
                DropdownMenuItem(
                    text = { Text("WebDAV 服务器") },
                    onClick = {
                        menuExpanded = false
                        // 用设置里的默认值初始化
                        WebDavStateHolder.state.value =
                            WebDavStateHolder.state.value.copy(
                                sharePath = settings.webDavSharePath,
                                port = settings.webDavPort,
                                username = settings.webDavUsername,
                                password = settings.webDavPassword
                            )
                        vm.openWebDav()
                    }
                )
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    onClick = {
                        menuExpanded = false
                        vm.openNewFolderDialog(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("新建文件") },
                    onClick = {
                        menuExpanded = false
                        vm.openNewFilePrompt(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    onClick = {
                        menuExpanded = false
                        vm.openSortDialog(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件")
                    },
                    onClick = {
                        menuExpanded = false
                        vm.setShowHidden(isLeft, !showHidden)
                    }
                )
                DropdownMenuItem(
                    text = { Text("刷新") },
                    onClick = {
                        menuExpanded = false
                        vm.refresh(isLeft)
                    }
                )
                DropdownMenuItem(
                    text = { Text("设置") },
                    onClick = {
                        menuExpanded = false
                        vm.openSettings()
                    }
                )
            }
        }
    }
}