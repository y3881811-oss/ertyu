package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.engine.AppLogger
import com.ertyu.model.LogStateHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewerScreen(vm: MainViewModel) {
    val state by LogStateHolder.state.collectAsState()
    var showDirDialog by remember { mutableStateOf(false) }
    var inputPath by remember { mutableStateOf(AppLogger.getCustomPath() ?: "") }

    BackHandler { vm.closeLogViewer() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运行日志") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeLogViewer() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { inputPath = AppLogger.getCustomPath() ?: ""; showDirDialog = true }) {
                        Icon(Icons.Default.Settings, "设置目录")
                    }
                    IconButton(onClick = { vm.refreshLogs() }) {
                        Icon(Icons.Default.Refresh, "刷新")
                    }
                    IconButton(onClick = { vm.exportLogs() }, enabled = !state.isExporting) {
                        Icon(Icons.Default.ExitToApp, "导出")
                    }
                    IconButton(onClick = { vm.clearLogs() }) {
                        Icon(Icons.Default.Delete, "清空")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { vm.updateLogSearch(it) },
                placeholder = { Text("搜索...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.updateLogSearch("") }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                singleLine = true
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = state.filterLevel == null, onClick = { vm.setLogFilter(null) }, label = { Text("全部") })
                FilterChip(selected = state.filterLevel == "E", onClick = { vm.setLogFilter("E") }, label = { Text("错误") })
                FilterChip(selected = state.filterLevel == "W", onClick = { vm.setLogFilter("W") }, label = { Text("警告") })
                FilterChip(selected = state.filterLevel == "I", onClick = { vm.setLogFilter("I") }, label = { Text("信息") })
            }
            HorizontalDivider(Modifier.padding(top = 8.dp))
            if (state.filteredLogs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无日志", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredLogs) { log -> LogRow(log) }
                }
            }
        }
    }

    if (showDirDialog) {
        AlertDialog(
            onDismissRequest = { showDirDialog = false },
            title = { Text("自定义日志目录") },
            text = {
                Column {
                    Text("绝对路径（留空恢复默认）", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = inputPath,
                        onValueChange = { inputPath = it },
                        placeholder = { Text("/storage/emulated/0/MyLogs") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("当前: ${AppLogger.getLogDir().absolutePath}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateLogDirectory(if (inputPath.isBlank()) null else inputPath.trim())
                    showDirDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showDirDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
fun LogRow(log: com.ertyu.model.LogEntry) {
    val levelColor = when (log.level) {
        "E" -> Color(0xFFF44336)
        "W" -> Color(0xFFFF9800)
        "I" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
    Column(
        Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[${log.level}]", color = levelColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, modifier = Modifier.width(36.dp))
            Text(log.time, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            Text(log.module, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(4.dp))
        Text(log.message, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
    }
}