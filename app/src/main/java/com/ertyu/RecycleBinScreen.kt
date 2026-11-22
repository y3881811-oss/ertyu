package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.RecycleBinItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(vm: MainViewModel) {
    val state by RecycleBinStateHolder.state.collectAsState()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    var showMenu by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmPurge by remember { mutableStateOf<Int?>(null) }   // days
    var confirmDeleteSelected by remember { mutableStateOf(false) }
    var confirmRestoreSelected by remember { mutableStateOf(false) }

    BackHandler {
        if (state.selected.isNotEmpty()) vm.clearRecycleSelection() else vm.closeRecycleBin()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("回收站") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selected.isNotEmpty()) vm.clearRecycleSelection() else vm.closeRecycleBin()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { vm.selectAllRecycleItems() }) {
                            Text("全选", style = MaterialTheme.typography.labelLarge)
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("清理 7 天前的条目") },
                                    onClick = { showMenu = false; confirmPurge = 7 }
                                )
                                DropdownMenuItem(
                                    text = { Text("清理 30 天前的条目") },
                                    onClick = { showMenu = false; confirmPurge = 30 }
                                )
                                DropdownMenuItem(
                                    text = { Text("清理 90 天前的条目") },
                                    onClick = { showMenu = false; confirmPurge = 90 }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("清空回收站", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; confirmClear = true }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("回收站是空的", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Text(
                    text = "共 ${state.items.size} 项 · 占用 ${ItemSizeEngine.format(state.totalBytes)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                if (state.working) LinearProgressIndicator(Modifier.fillMaxWidth())

                LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        val selected = item.id in state.selected
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { vm.toggleRecycleSelection(item.id) }
                                .padding(start = 4.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = selected, onCheckedChange = { vm.toggleRecycleSelection(item.id) })
                            Icon(
                                if (item.isDirectory) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "${dateFormat.format(Date(item.deleteTime))} · ${ItemSizeEngine.format(item.size)}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    item.originalPath,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { vm.restoreFromTrash(item) }) {
                                Icon(Icons.Default.Restore, contentDescription = "还原", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { vm.deleteFromTrash(item) }) {
                                Icon(Icons.Default.DeleteForever, contentDescription = "彻底删除", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        HorizontalDivider()
                    }
                }

                // ================= 多选操作条 =================
                if (state.selected.isNotEmpty()) {
                    Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "已选 ${state.selected.size} 项 · ${ItemSizeEngine.format(state.selectedBytes())}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(onClick = { vm.clearRecycleSelection() }) { Text("取消") }
                            TextButton(
                                onClick = { confirmRestoreSelected = true },
                                enabled = !state.working
                            ) { Text("还原") }
                            TextButton(
                                onClick = { confirmDeleteSelected = true },
                                enabled = !state.working
                            ) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    // ================= 各种确认 =================
    if (confirmRestoreSelected) {
        AlertDialog(
            onDismissRequest = { confirmRestoreSelected = false },
            title = { Text("还原选中项") },
            text = {
                Column {
                    Text("将还原 ${state.selected.size} 项到它们原来的位置。")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "如果原位置已有同名文件，会自动改名成「原名 (恢复)」，不会覆盖现有文件。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRestoreSelected = false
                    vm.restoreSelectedRecycleItems()
                }) { Text("还原") }
            },
            dismissButton = { TextButton(onClick = { confirmRestoreSelected = false }) { Text("取消") } }
        )
    }

    if (confirmDeleteSelected) {
        AlertDialog(
            onDismissRequest = { confirmDeleteSelected = false },
            title = { Text("彻底删除") },
            text = { Text("将永久删除选中的 ${state.selected.size} 项，无法恢复！") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteSelected = false
                    vm.deleteSelectedRecycleItems()
                }) { Text("永久删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteSelected = false }) { Text("取消") } }
        )
    }

    confirmPurge?.let { days ->
        val count = state.items.count { it.deleteTime < System.currentTimeMillis() - days.toLong() * 24 * 3600 * 1000 }
        AlertDialog(
            onDismissRequest = { confirmPurge = null },
            title = { Text("清理 $days 天前的条目") },
            text = {
                Text(
                    if (count == 0) "没有 $days 天前放入的条目。"
                    else "将永久删除 $count 项（放入回收站超过 $days 天），无法恢复！"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = count > 0,
                    onClick = {
                        confirmPurge = null
                        vm.purgeRecycleOlderThan(days)
                    }
                ) { Text("清理", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmPurge = null }) { Text("取消") } }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空回收站") },
            text = {
                Text("将永久删除全部 ${state.items.size} 项（${ItemSizeEngine.format(state.totalBytes)}），无法恢复！")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearRecycleBin()
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } }
        )
    }
}
