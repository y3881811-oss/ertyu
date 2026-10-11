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
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

private val I_FOLDER: ImageVector = Icons.Default.Folder
private val I_FILE: ImageVector = Icons.AutoMirrored.Filled.InsertDriveFile
private val I_MORE: ImageVector = Icons.Default.MoreVert
private val I_UP: ImageVector = Icons.Default.ArrowUpward
private val I_SWAP: ImageVector = Icons.Default.SwapHoriz
private val I_CLOSE: ImageVector = Icons.Default.Close
private val I_SELECT_ALL: ImageVector = Icons.Default.SelectAll
private val I_COPY: ImageVector = Icons.Default.ContentCopy
private val I_MOVE: ImageVector = Icons.AutoMirrored.Filled.DriveFileMove
private val I_DELETE: ImageVector = Icons.Default.Delete
private val I_NEW_FOLDER: ImageVector = Icons.Default.CreateNewFolder
private val I_NEW_FILE: ImageVector = Icons.Default.Add
private val I_SORT: ImageVector = Icons.AutoMirrored.Filled.Sort
private val I_REFRESH: ImageVector = Icons.Default.Refresh
private val I_VISIBILITY: ImageVector = Icons.Default.Visibility
private val I_VISIBILITY_OFF: ImageVector = Icons.Default.VisibilityOff
private val I_SEARCH: ImageVector = Icons.Default.Search
private val I_BOOKMARK: ImageVector = Icons.Default.Bookmark
private val I_ANDROID: ImageVector = Icons.Default.Android

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
            SelectionToolbar(
                count = state.selectedUris.size,
                onClear = { vm.clearSelection(isLeft) },
                onSelectAll = { vm.selectAll(isLeft) },
                onCopy = { vm.batchCopy(isLeft) },
                onMove = { vm.batchMove(isLeft) },
                onDelete = { vm.batchDelete(isLeft) },
                onZip = { vm.requestCompress(isLeft) }
            )
        } else {
            NormalToolbar(
                path = state.currentUri?.path ?: "未选择目录",
                canGoUp = state.canGoUp,
                showHidden = state.showHidden,
                showSwitch = onSwitchPanel != null,
                onUp = { vm.goUp(isLeft) },
                onPathClick = { vm.openJumpDialog(isLeft) },
                onNewFolder = { vm.openNewFolderDialog(isLeft) },
                onNewFile = { vm.openNewFilePrompt(isLeft) },
                onSort = { vm.openSortDialog(isLeft) },
                onToggleHidden = { vm.setShowHidden(isLeft, !state.showHidden) },
                onRefresh = { vm.refresh(isLeft) },
                onSwitchPanel = { onSwitchPanel?.invoke() },
                onSearch = { vm.openSearch(isLeft) },
                onAddBookmark = { vm.addBookmark(isLeft) },
                onOpenBookmarks = { vm.openBookmarkPanel(isLeft) },
                onInstalledApps = { vm.openInstalledApps() }
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(
                    items = state.items,
                    key = { it.uriString },
                    contentType = { if (it.isDirectory) "dir" else "file" }
                ) { item ->
                    val isSelected = state.selectedUris.contains(item.uriString)
                    FileRow(
                        item = item,
                        isSelected = isSelected,
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
private fun NormalToolbar(
    path: String,
    canGoUp: Boolean,
    showHidden: Boolean,
    showSwitch: Boolean,
    onUp: () -> Unit,
    onPathClick: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: () -> Unit,
    onSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchPanel: () -> Unit,
    onSearch: () -> Unit,
    onAddBookmark: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onInstalledApps: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUp, enabled = canGoUp) {
            Icon(I_UP, contentDescription = "上级")
        }
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { onPathClick() }
        )
        if (showSwitch) {
            IconButton(onClick = onSwitchPanel) {
                Icon(I_SWAP, contentDescription = "切换面板")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(I_MORE, contentDescription = "菜单")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("搜索") },
                    leadingIcon = { Icon(I_SEARCH, null) },
                    onClick = { menuExpanded = false; onSearch() }
                )
                DropdownMenuItem(
                    text = { Text("书签") },
                    leadingIcon = { Icon(I_BOOKMARK, null) },
                    onClick = { menuExpanded = false; onOpenBookmarks() }
                )
                DropdownMenuItem(
                    text = { Text("添加到书签") },
                    leadingIcon = { Icon(I_BOOKMARK, null) },
                    onClick = { menuExpanded = false; onAddBookmark() }
                )
                DropdownMenuItem(
                    text = { Text("已安装应用") },
                    leadingIcon = { Icon(I_ANDROID, null) },
                    onClick = { menuExpanded = false; onInstalledApps() }
                )
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    leadingIcon = { Icon(I_NEW_FOLDER, null) },
                    onClick = { menuExpanded = false; onNewFolder() }
                )
                DropdownMenuItem(
                    text = { Text("新建文件") },
                    leadingIcon = { Icon(I_NEW_FILE, null) },
                    onClick = { menuExpanded = false; onNewFile() }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    leadingIcon = { Icon(I_SORT, null) },
                    onClick = { menuExpanded = false; onSort() }
                )
                DropdownMenuItem(
                    text = {
                        Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件")
                    },
                    leadingIcon = {
                        Icon(
                            if (showHidden) I_VISIBILITY_OFF else I_VISIBILITY,
                            null
                        )
                    },
                    onClick = { menuExpanded = false; onToggleHidden() }
                )
                DropdownMenuItem(
                    text = { Text("刷新") },
                    leadingIcon = { Icon(I_REFRESH, null) },
                    onClick = { menuExpanded = false; onRefresh() }
                )
            }
        }
    }
}

@Composable
private fun SelectionToolbar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onZip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) {
            Icon(I_CLOSE, contentDescription = "取消")
        }
        Text(
            text = "已选 $count 项",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSelectAll) {
            Icon(I_SELECT_ALL, contentDescription = "全选")
        }
        IconButton(onClick = onZip) {
            Icon(I_NEW_FOLDER, contentDescription = "压缩")
        }
        IconButton(onClick = onCopy) {
            Icon(I_COPY, contentDescription = "复制")
        }
        IconButton(onClick = onMove) {
            Icon(I_MOVE, contentDescription = "移动")
        }
        IconButton(onClick = onDelete) {
            Icon(I_DELETE, contentDescription = "删除")
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
            imageVector = if (item.isDirectory) I_FOLDER else I_FILE,
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
                Icon(I_MORE, contentDescription = "更多")
            }
        }
    }
}