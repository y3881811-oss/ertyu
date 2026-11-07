package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveViewerScreen(vm: MainViewModel) {
    val state by ArchiveViewerStateHolder.state.collectAsState()
    var actionTarget by remember { mutableStateOf<ArchiveEntry?>(null) }
    
    // 👇 新增：控制“全部解压”弹窗的状态
    var showExtractDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.archiveName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (state.currentDir.isEmpty()) "/" else "/${state.currentDir}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeArchiveViewer() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { showExtractDialog = true }) { // 👈 点击弹出解压选项
                        Icon(Icons.Default.FolderOpen, contentDescription = "全部解压")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.filter,
                onValueChange = { vm.updateArchiveViewerFilter(it) },
                placeholder = { Text("全局过滤路径…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.filter.isNotEmpty()) {
                        IconButton(onClick = { vm.updateArchiveViewerFilter("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            HorizontalDivider()

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("错误: ${state.error}", color = MaterialTheme.colorScheme.error)
                }
                state.displayEntries.isEmpty() && state.currentDir.isNotEmpty() && state.filter.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("空文件夹")
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    if (state.filter.isEmpty() && state.currentDir.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { vm.goUpArchiveDir() }.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "..", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("..", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    items(state.displayEntries, key = { it.path }) { entry ->
                        ArchiveEntryRow(
                            entry = entry,
                            onClick = {
                                if (entry.isDirectory) {
                                    vm.enterArchiveDir(entry.path)
                                } else {
                                    actionTarget = entry
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // 提取单个文件的弹窗
    actionTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = { Text(entry.path, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            text = {
                Column {
                    Text("大小：${entry.sizeText}", style = MaterialTheme.typography.bodyMedium)
                    if (entry.compressedSize > 0) {
                        Text("压缩后：${entry.compressedText}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.extractArchiveEntry(entry)
                    actionTarget = null
                }) { Text("提取到当前目录") }
            },
            dismissButton = {
                TextButton(onClick = { actionTarget = null }) { Text("取消") }
            }
        )
    }

    // 👇 新增：全部解压的选项弹窗
    if (showExtractDialog) {
        var targetDirName by remember { mutableStateOf(state.archiveName.substringBeforeLast(".")) }
        var toOtherPanel by remember { mutableStateOf(false) }
        var deleteSource by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showExtractDialog = false },
            title = { Text("解压") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = !toOtherPanel, onClick = { toOtherPanel = false })
                        Text("解压到当前目录")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = toOtherPanel, onClick = { toOtherPanel = true })
                        Text("解压到另一标签页")
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = targetDirName,
                        onValueChange = { targetDirName = it },
                        label = { Text("目标文件夹名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteSource = !deleteSource }
                    ) {
                        Checkbox(checked = deleteSource, onCheckedChange = { deleteSource = it })
                        Spacer(Modifier.width(4.dp))
                        Text("解压后删除源文件")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.extractAllFromViewer(
                        targetDirName = targetDirName,
                        toOtherPanel = toOtherPanel,
                        deleteSource = deleteSource
                    )
                    showExtractDialog = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExtractDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArchiveEntryRow(
    entry: ArchiveEntry,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (entry.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.isDirectory && entry.sizeText.isNotEmpty()) {
                Text(
                    text = buildString {
                        append(entry.sizeText)
                        if (entry.compressedSize > 0) {
                            append(" · 压缩 ")
                            append(entry.compressedText)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}