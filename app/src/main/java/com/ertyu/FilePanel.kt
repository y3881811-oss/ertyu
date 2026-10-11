package com.ertyu

import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.FileItem
import com.ertyu.model.SortMode
import java.io.File

// ==================== 顶层图标缓存 ====================

private val IconFolder: ImageVector = Icons.Default.Folder
private val IconFile: ImageVector = Icons.AutoMirrored.Filled.InsertDriveFile
private val IconMore: ImageVector = Icons.Default.MoreVert
private val IconUp: ImageVector = Icons.Default.ArrowUpward
private val IconSwap: ImageVector = Icons.Default.SwapHoriz
private val IconClose: ImageVector = Icons.Default.Close
private val IconSelectAll: ImageVector = Icons.Default.SelectAll
private val IconCopy: ImageVector = Icons.Default.ContentCopy
private val IconMove: ImageVector = Icons.AutoMirrored.Filled.DriveFileMove
private val IconDelete: ImageVector = Icons.Default.Delete
private val IconNewFolder: ImageVector = Icons.Default.CreateNewFolder
private val IconSort: ImageVector = Icons.AutoMirrored.Filled.Sort
private val IconRefresh: ImageVector = Icons.Default.Refresh
private val IconEdit: ImageVector = Icons.Default.Edit
private val IconVisibility: ImageVector = Icons.Default.Visibility
private val IconVisibilityOff: ImageVector = Icons.Default.VisibilityOff
private val IconZip: ImageVector = Icons.Default.FolderZip
private val IconInfo: ImageVector = Icons.Default.Info
private val IconSearch: ImageVector = Icons.Default.Search
private val IconInstall: ImageVector = Icons.Default.InstallMobile

@Composable
fun FilePanel(
    isLeft: Boolean,
    vm: MainViewModel,
    onSwitchPanel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val panelFlow = if (isLeft) vm.leftPanel else vm.rightPanel
    val state by panelFlow.collectAsState()
    val compressPromptVisible by vm.compressPromptVisible.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<FileItem?>(null) }
    var actionTarget by remember { mutableStateOf<FileItem?>(null) }
    var infoTarget by remember { mutableStateOf<FileItem?>(null) }

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
                onPathClick = { showJumpDialog = true },
                onNewFolder = { showNewFolderDialog = true },
                onSort = { showSortDialog = true },
                onToggleHidden = { vm.setShowHidden(isLeft, !state.showHidden) },
                onRefresh = { vm.refresh(isLeft) },
                onSwitchPanel = { onSwitchPanel?.invoke() },
                onSearch = { vm.openSearch(isLeft) }
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
                Text(if (state.showHidden) "空目录" else "空目录（可尝试显示隐藏文件）")
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
                                actionTarget = item
                            }
                        },
                        onMoreClick = { actionTarget = item }
                    )
                }
            }
        }
    }

    actionTarget?.let { target ->
        val format = ArchiveFormat.detect(target.name)
        val isApk = target.name.substringAfterLast('.', "").lowercase() == "apk"
        FileActionDialog(
            item = target,
            archiveFormat = format,
            isApk = isApk,
            onDismiss = { actionTarget = null },
            onRename = {
                actionTarget = null
                renameTarget = target
            },
            onCopy = {
                actionTarget = null
                vm.copyToOtherPanel(isLeft, target)
            },
            onMove = {
                actionTarget = null
                vm.moveToOtherPanel(isLeft, target)
            },
            onDelete = {
                actionTarget = null
                vm.delete(isLeft, target)
            },
            onSelect = {
                actionTarget = null
                vm.enterSelection(isLeft, target.uriString)
            },
            onExtractHere = {
                actionTarget = null
                vm.extractZipHere(isLeft, target)
            },
            onExtractOther = {
                actionTarget = null
                vm.extractZipToOtherPanel(isLeft, target)
            },
            onZipThis = {
                actionTarget = null
                vm.enterSelection(isLeft, target.uriString)
                vm.requestCompress(isLeft)
            },
            onInfo = {
                actionTarget = null
                infoTarget = target
            },
            onInstall = {
                actionTarget = null
                vm.installApk(target)
            }
        )
    }

    infoTarget?.let { target ->
        FileInfoDialog(
            item = target,
            vm = vm,
            onDismiss = {
                vm.resetFolderSize()
                infoTarget = null
            }
        )
    }

    if (compressPromptVisible) {
        CompressFormatDialog(
            onSelect = { format -> vm.compressWithFormat(format) },
            onDismiss = { vm.dismissCompressPrompt() }
        )
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

@Composable
private fun CompressFormatDialog(
    onSelect: (ArchiveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择压缩格式") },
        text = {
            Column {
                CompressFormatItem(
                    label = "ZIP",
                    desc = ".zip（兼容性最好，Windows/手机通用）",
                    format = ArchiveFormat.ZIP,
                    onSelect = onSelect
                )
                CompressFormatItem(
                    label = "TAR",
                    desc = ".tar（不压缩，仅打包）",
                    format = ArchiveFormat.TAR,
                    onSelect = onSelect
                )
                CompressFormatItem(
                    label = "TAR.GZ",
                    desc = ".tar.gz（Linux 常见，压缩率高）",
                    format = ArchiveFormat.TAR_GZ,
                    onSelect = onSelect
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun CompressFormatItem(
    label: String,
    desc: String,
    format: ArchiveFormat,
    onSelect: (ArchiveFormat) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(format) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(IconZip, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FileInfoDialog(
    item: FileItem,
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val sizeState by vm.folderSizeState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件属性") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("名称", item.name)
                InfoRow("类型", item.typeText)

                if (item.isDirectory) {
                    val countText = if (item.childCount >= 0)
                        "目录（包含 ${item.childCount} 项）" else "目录"
                    InfoRow("内容", countText)

                    when {
                        sizeState.error != null -> {
                            InfoRow("总大小", "计算失败：${sizeState.error}")
                        }
                        sizeState.done -> {
                            InfoRow(
                                "总大小",
                                "${formatSize(sizeState.totalBytes)}（${sizeState.scanned} 个文件）"
                            )
                        }
                        sizeState.isCalculating -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "正在计算…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "已扫描 ${sizeState.scanned} 个文件 · ${formatSize(sizeState.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            TextButton(onClick = { vm.calculateFolderSize(item) }) {
                                Text("计算总大小")
                            }
                        }
                    }
                } else {
                    InfoRow("大小", "${item.sizeText}（${item.size} 字节）")
                }

                InfoRow("修改时间", item.lastModifiedText)
                InfoRow("路径", item.path)

                if (sizeState.isCalculating) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { vm.cancelFolderSize() }) {
                        Text("取消计算")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}

@Composable
private fun FileActionDialog(
    item: FileItem,
    archiveFormat: ArchiveFormat?,
    isApk: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractOther: () -> Unit,
    onZipThis: () -> Unit,
    onInfo: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isApk) {
                    ActionItem("安装", IconInstall, onInstall)
                }
                if (archiveFormat != null) {
                    ActionItem(
                        "解压到当前目录（${archiveFormat.label}）",
                        IconZip,
                        onExtractHere
                    )
                    ActionItem("解压到另一侧", IconZip, onExtractOther)
                }
                ActionItem("压缩为…", IconZip, onZipThis)
                ActionItem("属性", IconInfo, onInfo)
                ActionItem("重命名", IconEdit, onRename)
                ActionItem("复制到另一侧", IconCopy, onCopy)
                ActionItem("移动到另一侧", IconMove, onMove)
                ActionItem("删除", IconDelete, onDelete)
                ActionItem("多选", IconSelectAll, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
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
    onSort: () -> Unit,
    onToggleHidden: () -> Unit,
    onRefresh: () -> Unit,
    onSwitchPanel: () -> Unit,
    onSearch: () -> Unit
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
            Icon(IconUp, contentDescription = "上级")
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
                Icon(IconSwap, contentDescription = "切换面板")
            }
        }
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(IconMore, contentDescription = "菜单")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("搜索") },
                    leadingIcon = { Icon(IconSearch, null) },
                    onClick = {
                        menuExpanded = false
                        onSearch()
                    }
                )
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    leadingIcon = { Icon(IconNewFolder, null) },
                    onClick = {
                        menuExpanded = false
                        onNewFolder()
                    }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    leadingIcon = { Icon(IconSort, null) },
                    onClick = {
                        menuExpanded = false
                        onSort()
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件") },
                    leadingIcon = {
                        Icon(
                            if (showHidden) IconVisibilityOff else IconVisibility,
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
                    leadingIcon = { Icon(IconRefresh, null) },
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
            Icon(IconClose, contentDescription = "取消")
        }
        Text(
            text = "已选 $count 项",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSelectAll) {
            Icon(IconSelectAll, contentDescription = "全选")
        }
        IconButton(onClick = onZip) {
            Icon(IconZip, contentDescription = "压缩")
        }
        IconButton(onClick = onCopy) {
            Icon(IconCopy, contentDescription = "复制到另一侧")
        }
        IconButton(onClick = onMove) {
            Icon(IconMove, contentDescription = "移动到另一侧")
        }
        IconButton(onClick = onDelete) {
            Icon(IconDelete, contentDescription = "删除")
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
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    val subtitle = remember(item.uriString, item.sizeText, item.typeText, item.lastModifiedText) {
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
            imageVector = if (item.isDirectory) IconFolder else IconFile,
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
                Icon(IconMore, contentDescription = "更多")
            }
        }
    }
}

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