package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.DiffKind
import com.ertyu.engine.DirDiffEntry
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.DirCompareHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirCompareScreen(vm: MainViewModel) {
    val state by DirCompareHolder.state.collectAsState()
    var pendingToRight by remember { mutableStateOf<Boolean?>(null) }

    BackHandler { vm.closeDirCompare() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目录对比与同步") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeDirCompare() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.startDirCompare() }, enabled = !state.isScanning && !state.isSyncing) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新对比")
                    }
                    TextButton(onClick = { vm.selectAllDirDiffs() }, enabled = !state.isScanning) {
                        Text("全选差异")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ================= 顶部信息 =================
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    RootLine("左", state.leftRoot)
                    Spacer(Modifier.height(2.dp))
                    RootLine("右", state.rightRoot)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CountChip("仅左 ${state.leftOnly}", DiffKind.LEFT_ONLY)
                        CountChip("仅右 ${state.rightOnly}", DiffKind.RIGHT_ONLY)
                        CountChip("不同 ${state.different}", DiffKind.DIFFERENT)
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = state.onlyDiff,
                            onCheckedChange = { vm.setDirCompareOnlyDiff(it) }
                        )
                        Text("只看差异", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(12.dp))
                        Checkbox(
                            checked = state.deep,
                            onCheckedChange = { vm.setDirCompareDeep(it) }
                        )
                        Text("比较修改时间/大小", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        text = state.progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (state.isScanning || state.isSyncing) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.error?.let {
                Text(
                    "出错了：$it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ================= 列表 =================
            val list = state.visibleEntries
            Box(Modifier.weight(1f)) {
                if (list.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (state.isScanning) "正在对比…" else "两侧完全一致 🎉",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                        items(list, key = { it.relPath }) { entry ->
                            DirDiffRow(
                                entry = entry,
                                selected = entry.relPath in state.selected,
                                onClick = { vm.toggleDirDiffSelection(entry.relPath) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            // ================= 底部同步条 =================
            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.selected.isEmpty()) "同步全部差异" else "已选 ${state.selected.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.selected.isNotEmpty()) {
                        TextButton(onClick = { vm.clearDirDiffSelection() }) { Text("清空") }
                    }
                    TextButton(
                        onClick = { pendingToRight = false },
                        enabled = !state.isSyncing && !state.isScanning
                    ) { Text("← 复制到左侧") }
                    TextButton(
                        onClick = { pendingToRight = true },
                        enabled = !state.isSyncing && !state.isScanning
                    ) { Text("复制到右侧 →") }
                }
            }
        }
    }

    pendingToRight?.let { toRight ->
        AlertDialog(
            onDismissRequest = { pendingToRight = null },
            title = { Text(if (toRight) "复制到右侧" else "复制到左侧") },
            text = {
                Column {
                    Text(
                        if (state.selected.isEmpty())
                            "将把全部差异项复制到${if (toRight) "右" else "左"}侧（目标侧独有的文件不会被删除）。"
                        else
                            "将把选中的 ${state.selected.size} 项复制到${if (toRight) "右" else "左"}侧。"
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "已有的同名文件会被覆盖。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dir = toRight
                    pendingToRight = null
                    vm.syncDirDiffs(dir)
                }) { Text("开始同步") }
            },
            dismissButton = {
                TextButton(onClick = { pendingToRight = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RootLine(tag: String, path: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            tag,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = path.ifBlank { "—" },
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CountChip(text: String, kind: DiffKind) {
    val color = kindColor(kind)
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun kindColor(kind: DiffKind): Color = when (kind) {
    DiffKind.LEFT_ONLY -> Color(0xFF2E7D32)
    DiffKind.RIGHT_ONLY -> Color(0xFF1565C0)
    DiffKind.DIFFERENT -> Color(0xFFEF6C00)
    DiffKind.SAME -> Color(0xFF757575)
}

@Composable
private fun DirDiffRow(entry: DirDiffEntry, selected: Boolean, onClick: () -> Unit) {
    val color = kindColor(entry.kind)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = { onClick() })
        Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.15f)) {
            Text(
                entry.kind.label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.relPath + if (entry.isDirectory) "/" else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val leftText = if (entry.leftPath.isNotEmpty() && entry.kind != DiffKind.RIGHT_ONLY) ItemSizeEngine.format(entry.leftSize) else "—"
            val rightText = if (entry.kind != DiffKind.LEFT_ONLY) ItemSizeEngine.format(entry.rightSize) else "—"
            Text(
                text = "左 $leftText   右 $rightText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
