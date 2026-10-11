package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

@Composable
fun FilePanel(
    isLeft: Boolean,
    vm: MainViewModel,
    onSwitchPanel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()

    Column(modifier = modifier.fillMaxHeight()) {
        if (state.isSelectionMode) {
            // 多选工具栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { vm.clearSelection(isLeft) }) {
                    Icon(Icons.Default.Close, contentDescription = "取消")
                }
                Text(
                    text = "已选 ${state.selectedUris.size} 项",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { vm.selectAll(isLeft) }) {
                    Icon(Icons.Default.SelectAll, contentDescription = "全选")
                }
                IconButton(onClick = { vm.requestCompress(isLeft) }) {
                    Icon(Icons.Default.Folder, contentDescription = "压缩")
                }
                IconButton(onClick = { vm.batchCopy(isLeft) }) {
                    Icon(Icons.Default.Add, contentDescription = "复制")
                }
                IconButton(onClick = { vm.batchMove(isLeft) }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "移动")
                }
                IconButton(onClick = { vm.batchDelete(isLeft) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        } else {
            // 普通工具栏
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

        // 内容
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
                                vm.openDirectory(isLeft, item.uri)
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
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer
    else Color.Transparent

    val subtitle = remember(
        item.uriString,
        item.sizeText,
        item.typeText,
        item.lastModifiedText
    ) {
        buildString {
            append(item.typeText)
            if (!item.isDirectory && item.sizeText.isNotEmpty()) {
                append(" · ")
                append(item.sizeText)
            }
            if (item.lastModifiedText.isNotEmpty()) {
                append(" · ")
                append(item.lastModifiedText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder
            else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isSelectionMode) {
            IconButton(
                onClick = onMoreClick,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}