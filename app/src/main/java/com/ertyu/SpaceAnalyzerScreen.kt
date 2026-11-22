package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.ItemSizePrefs
import com.ertyu.model.ItemSizeHolder
import com.ertyu.model.SpaceAnalyzerHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceAnalyzerScreen(vm: MainViewModel) {
    val state by SpaceAnalyzerHolder.state.collectAsState()
    val showFolderSize by ItemSizeHolder.enabled.collectAsState()
    val context = LocalContext.current

    var tab by remember { mutableIntStateOf(0) }
    var showRootDialog by remember { mutableStateOf(false) }
    var rootInput by remember(state.root) { mutableStateOf(state.root) }

    BackHandler { vm.closeSpaceAnalyzer() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("空间分析") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeSpaceAnalyzer() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.startSpaceScan() }, enabled = !state.isScanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新扫描")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ================= 顶部：扫描配置 =================
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "目录",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.root,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { rootInput = state.root; showRootDialog = true }) {
                            Text("更改")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            "大文件阈值",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(10, 50, 100, 500).forEach { mb ->
                            FilterChip(
                                selected = state.minSizeMb == mb,
                                onClick = { vm.startSpaceScan(mb) },
                                label = { Text(">${mb}MB") }
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.isScanning) {
                            TextButton(onClick = { vm.stopSpaceScan() }) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("停止")
                            }
                        } else {
                            TextButton(onClick = { vm.startSpaceScan() }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(if (state.finished) "重新扫描" else "开始扫描")
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "已扫描 ${state.scannedFiles} 个文件",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "合计 ${ItemSizeEngine.format(state.totalBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (state.isScanning) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                }
            }

            // ================= 文件夹大小开关 =================
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "在文件列表显示文件夹大小",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = showFolderSize,
                    onCheckedChange = { ItemSizePrefs.setEnabled(context, it) }
                )
            }

            state.error?.let {
                Text(
                    text = "出错了：$it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            PrimaryTabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("大文件 (${state.bigFiles.size})") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("目录占用 (${state.topFolders.size})") })
            }

            if (tab == 0) {
                BigFileList(vm)
            } else {
                FolderUsageList(vm)
            }
        }
    }

    if (showRootDialog) {
        AlertDialog(
            onDismissRequest = { showRootDialog = false },
            title = { Text("扫描目录") },
            text = {
                Column {
                    OutlinedTextField(
                        value = rootInput,
                        onValueChange = { rootInput = it },
                        singleLine = true,
                        placeholder = { Text("/storage/emulated/0") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "提示：扫描 /storage/emulated/0 会遍历整块内部存储，耗时较长。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = rootInput.trim()
                    showRootDialog = false
                    if (p.isNotEmpty()) {
                        vm.setSpaceAnalyzerRoot(p)
                        vm.startSpaceScan()
                    }
                }) { Text("开始") }
            },
            dismissButton = {
                TextButton(onClick = { showRootDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun BigFileList(vm: MainViewModel) {
    val state by SpaceAnalyzerHolder.state.collectAsState()

    if (state.bigFiles.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (state.isScanning) "正在扫描…" else "没有大于 ${state.minSizeMb}MB 的文件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(state.bigFiles, key = { it.path }) { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.revealAmongFiles(entry.path) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        entry.path.substringBeforeLast('/'),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    ItemSizeEngine.format(entry.size),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun FolderUsageList(vm: MainViewModel) {
    val state by SpaceAnalyzerHolder.state.collectAsState()
    val total = state.totalBytes.coerceAtLeast(1L)

    if (state.topFolders.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (state.isScanning) "正在扫描…" else "还没有结果，点“开始扫描”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(state.topFolders, key = { it.path }) { f ->
            val fraction = (f.bytes.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        vm.setSpaceAnalyzerRoot(f.path)
                        vm.startSpaceScan()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = f.path.substringAfterLast('/'),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        ItemSizeEngine.format(f.bytes),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${f.fileCount} 个文件 · ${(fraction * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}
