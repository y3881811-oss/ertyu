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
    var showCloseConfirm by remember { mutableStateOf(false) }
    val busy = state.isScanning || state.isSyncing

    fun requestClose() {
        if (state.isSyncing) showCloseConfirm = true else vm.closeDirCompare()
    }

    BackHandler { requestClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("目录对比与同步") },
                navigationIcon = {
                    IconButton(onClick = { requestClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.startDirCompare() }, enabled = !busy) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新对比")
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
                            onCheckedChange = { vm.setDirCompareOnlyDiff(it) },
                            enabled = !state.isSyncing
                        )
                        Text("只看差异", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(10.dp))
                        Checkbox(
                            checked = state.deep,
                            onCheckedChange = { vm.setDirCompareDeep(it) },
                            enabled = !busy
                        )
                        Text("比大小和修改时间", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = state.progressText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (state.isSyncing) {
                            TextButton(onClick = { vm.cancelDirDiffSync() }) {
                                Text("停止", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())

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
                            text = when {
                                state.isScanning -> "正在对比…"
                                state.error != null -> "对比失败，可点右上角重试"
                                else -> "两侧完全一致 🎉"
                            },
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
                                enabled = !state.isSyncing,
                                onClick = { vm.toggleDirDiffSelection(entry.relPath) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }

            // ================= 底部：选择 + 同步 =================
            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.selected.isEmpty()) "未勾选：按全部差异处理"
                            else "已勾选 ${state.selected.size} 项",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        TextButton(
                            onClick = { vm.selectAllDirDiffs() },
                            enabled = !busy && list.any { it.kind != DiffKind.SAME }
                        ) { Text("全选") }
                        TextButton(
                            onClick = { vm.invertDirDiffSelection() },
                            enabled = !busy && list.any { it.kind != DiffKind.SAME }
                        ) { Text("反选") }
                        TextButton(
                            onClick = { vm.clearDirDiffSelection() },
                            enabled = !busy && state.selected.isNotEmpty()
                        ) { Text("清空") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val toLeftCount = state.targets(false).size
                        val toRightCount = state.targets(true).size
                        FilledTonalButton(
                            onClick = { pendingToRight = false },
                            enabled = !busy && toLeftCount > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("← 同步到左侧 ($toLeftCount)", maxLines = 1) }
                        FilledTonalButton(
                            onClick = { pendingToRight = true },
                            enabled = !busy && toRightCount > 0,
                            modifier = Modifier.weight(1f)
                        ) { Text("同步到右侧 → ($toRightCount)", maxLines = 1) }
                    }
                    val skippedRight = state.skipped(true)
                    if (state.selected.isNotEmpty() && (state.skipped(false) > 0 || skippedRight > 0)) {
                        Text(
                            text = "提示：勾选里有一部分只存在于目标侧（没有来源），不会参与同步。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // ================= 同步确认 =================
    pendingToRight?.let { toRight ->
        val targets = state.targets(toRight)
        val bytes = state.sourceBytes(toRight)
        val overwrite = state.overwriteCount(toRight)
        val dirCount = targets.count { it.isDirectory }
        AlertDialog(
            onDismissRequest = { pendingToRight = null },
            title = { Text(if (toRight) "同步到右侧" else "同步到左侧") },
            text = {
                Column {
                    Text(
                        text = if (state.selected.isEmpty()) "范围：全部差异"
                        else "范围：已勾选的 ${state.selected.size} 项",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("将复制 ${targets.size - dirCount} 个文件" + if (dirCount > 0) "、创建 $dirCount 个文件夹" else "")
                    Text("总大小约 ${ItemSizeEngine.format(bytes)}")
                    if (overwrite > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "$overwrite 项目标侧已存在，会被覆盖",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "目标侧独有的文件不会被删除，只会新增或覆盖。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val dir = toRight
                    pendingToRight = null
                    vm.syncDirDiffs(dir)
                }) { Text(if (overwrite > 0) "覆盖并同步" else "开始同步") }
            },
            dismissButton = {
                TextButton(onClick = { pendingToRight = null }) { Text("取消") }
            }
        )
    }

    // ================= 同步中退出确认 =================
    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("正在同步") },
            text = { Text("退出会中止同步，已经复制的文件不会回滚。确定退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirm = false
                    vm.closeDirCompare()
                }) { Text("中止并退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("继续同步") }
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
    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.15f)) {
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
private fun DirDiffRow(
    entry: DirDiffEntry,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color = kindColor(entry.kind)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = selected, onCheckedChange = { if (enabled) onClick() }, enabled = enabled)
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
            val leftText = if (entry.kind == DiffKind.RIGHT_ONLY) "—" else ItemSizeEngine.format(entry.leftSize)
            val rightText = if (entry.kind == DiffKind.LEFT_ONLY) "—" else ItemSizeEngine.format(entry.rightSize)
            Text(
                text = "左 $leftText   右 $rightText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
