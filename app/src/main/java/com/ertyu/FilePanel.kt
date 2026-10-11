package com.ertyu

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.ertyu.model.SortMode
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FilePanel(
    isLeft: Boolean,
    vm: MainViewModel,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }

    Column(modifier = modifier.fillMaxHeight()) {

        if (state.isSelectionMode) {
            SelectionToolbar(
                count = state.selectedUris.size,
                onClear = { vm.clearSelection(isLeft) },
                onSelectAll = { vm.selectAll(isLeft) },
                onCopy = { vm.batchCopy(isLeft) },
                onMove = { vm.batchMove(isLeft) },
                onDelete = { vm.batchDelete(isLeft) }
            )
        } else {
            NormalToolbar(
                path = state.currentUri?.path ?: "未选择目录",
                canGoUp = state.canGoUp,
                showHidden = state.showHidden,
                onUp = { vm.goUp(isLeft) },
                onPathClick = { showJumpDialog = true },
                onNewFolder = { showNewFolderDialog = true },
                onSort = { showSortDialog = true },
                onToggleHidden = { vm.setShowHidden(isLeft, !state.showHidden) },
                onRefresh = { vm.refresh(isLeft) }
            )
        }
        HorizontalDivider()

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
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.uri.toString() }) { item ->
                    FileRow(
                        item = item,
                        isSelected = state.selectedUris.contains(item.uri.toString()),
                        isSelectionMode = state.isSelectionMode,
                        onClick = {
                            if (state.isSelectionMode) {
                                vm.toggleSelection(isLeft, item.uri.toString())
                            } else if (item.isDirectory) {
                                vm.openDirectory(isLeft, item.uri)
                            }
                        },
                        onLongClick = {
                            if (!state.isSelectionMode) {
                                vm.enterSelection(isLeft, item.uri.toString())
                            }
                        },
                        onRename = { renameTarget = item },
                        onCopy = { vm.copyToOtherPanel(isLeft, item) },
                        onDelete = { vm.delete(isLeft, item) }
                    )
                }
            }
        }
    }

    if (showSortDialog) {
        SortDialog(
            current = state.sortMode,
            onSelect = {
                vm.setSortMode(isLeft, it)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    if (showNewFolderDialog) {
        TextInputDialog(
            title = "新建文件夹",
            placeholder = "文件夹名称",
            initial = "",
            onConfirm = { name ->
                if (name.isNotBlank()) vm.createDirectory(isLeft, name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }

    if (showJumpDialog) {
        TextInputDialog(
            title = "跳转到路径",
            placeholder = "/storage/emulated/0/...",
            initial = state.currentUri?.path ?: "",
            onConfirm = { input ->
                if (input.isNotBlank()) {
                    val dir = File(input)
                    if (dir.exists() && dir.isDirectory) {
                        vm.openDirectory(isLeft, Uri.fromFile(dir))
                    }
                }
                showJumpDialog = false
            },
            onDismiss = { showJumpDialog = false }
        )
    }

    renameTarget?.let { target ->
        TextInputDialog(
            title = "重命名",
            placeholder = "新名称",
            initial = target.name,
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != target.name) {
                    vm.rename(isLeft, target, newName)
                }
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
}

// ==================== 顶部工具栏 ====================

@Composable
private fun NormalToolbar(
    path: String,
    canGoUp: Boolean,
    showHidden: Boolean,
    onUp: () -> Unit,
    onPathClick: () -> Unit,
    onNewFolder: () -> Unit,
    onSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onUp, enabled = canGoUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上级")
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
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "菜单")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, null) },
                    onClick = {
                        menuExpanded = false
                        onNewFolder()
                    }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    leadingIcon = { Icon(Icons.Default.Sort, null) },
                    onClick = {
                        menuExpanded = false
                        onSort()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件") },
                    leadingIcon = {
                        Icon(
                            if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onToggleHidden()
                    }
                )
                DropdownMenuItem(
                    text = { Text("刷新") },
                    leadingIcon = { Icon(Icons.Default.Refresh, null) },
                    onClick = {
                        menuExpanded = false
                        onRefresh()
                    }
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
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = "取消")
        }
        Text(
            text = "已选 $count 项",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSelectAll) {
            Icon(Icons.Default.SelectAll, contentDescription = "全选")
        }
        IconButton(onClick = onCopy) {
            Icon(Icons.Default.ContentCopy, contentDescription = "复制到另一侧")
        }
        IconButton(onClick = onMove) {
            Icon(Icons.Default.DriveFileMove, contentDescription = "移动到另一侧")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除")
        }
    }
}

// ==================== 文件行 ====================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileRow(
    item: FileItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    ListItem(
        headlineContent = {
            Text(item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (!item.isDirectory) Text(formatSize(item.size))
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder
                else Icons.Default.InsertDriveFile,
                contentDescription = null
            )
        },
        trailingContent = {
            if (!isSelectionMode) {
                Row {
                    IconButton(onClick = onRename) {
                        Icon(Icons.Default.Edit, contentDescription = "重命名")
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "复制到另一侧")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
        },
        modifier = Modifier
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

// ==================== 对话框 ====================

@Composable
private fun TextInputDialog(
    title: String,
    placeholder: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun SortDialog(
    current: SortMode,
    onSelect: (SortMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                SortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

// ==================== 工具 ====================

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}