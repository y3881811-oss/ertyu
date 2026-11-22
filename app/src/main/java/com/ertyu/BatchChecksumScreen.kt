package com.ertyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.BatchChecksumHolder
import com.ertyu.model.ChecksumAlgorithm
import com.ertyu.model.ChecksumRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchChecksumScreen(vm: MainViewModel) {
    val state by BatchChecksumHolder.state.collectAsState()
    val context = LocalContext.current
    var showCloseConfirm by remember { mutableStateOf(false) }

    fun requestClose() {
        if (state.running) showCloseConfirm = true else vm.closeBatchChecksum()
    }

    BackHandler { requestClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量校验哈希") },
                navigationIcon = {
                    IconButton(onClick = { requestClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.exportChecksumCsv() },
                        enabled = state.rows.any { it.hash != null }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "导出 CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // ================= 算法选择 =================
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChecksumAlgorithm.values().forEach { alg ->
                    FilterChip(
                        selected = state.algorithm == alg,
                        onClick = { vm.setChecksumAlgorithm(alg) },
                        label = { Text(alg.label) },
                        enabled = !state.running,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            if (state.running) {
                LinearProgressIndicator(
                    progress = {
                        if (state.rows.isEmpty()) 0f
                        else state.progressIndex.toFloat() / state.rows.size.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
            }
            Text(
                text = if (state.running) "正在计算 ${state.progressIndex}/${state.rows.size}"
                else "共 ${state.rows.size} 项，已完成 ${state.doneCount} 项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            state.error?.let {
                Text(
                    "出错了：$it",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Box(Modifier.weight(1f)) {
                if (state.rows.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("请先在文件列表里选中文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.rows, key = { it.path }) { row ->
                            ChecksumRowItem(row, onCopy = { hash -> copyToClipboard(context, row.name, hash) })
                            HorizontalDivider()
                        }
                    }
                }
            }

            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.running) {
                        TextButton(onClick = { vm.cancelBatchChecksum() }) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("停止")
                        }
                    } else {
                        TextButton(onClick = { vm.startBatchChecksum() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(if (state.doneCount == state.rows.size) "重新计算" else "开始计算")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = { vm.exportChecksumCsv() },
                        enabled = state.rows.any { it.hash != null }
                    ) { Text("导出 CSV") }
                }
            }
        }
    }

    if (showCloseConfirm) {
        AlertDialog(
            onDismissRequest = { showCloseConfirm = false },
            title = { Text("正在计算") },
            text = { Text("退出会停止校验，已完成的结果不会保存。确定退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showCloseConfirm = false
                    vm.closeBatchChecksum()
                }) { Text("停止并退出", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirm = false }) { Text("继续计算") }
            }
        )
    }
}

@Composable
private fun ChecksumRowItem(row: ChecksumRow, onCopy: (String) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = row.hash != null) { row.hash?.let(onCopy) }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = ItemSizeEngine.format(row.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        when {
            row.hash != null -> Text(
                text = row.hash,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            row.error != null -> Text(
                text = row.error,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
            else -> Text(
                text = "等待计算…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Exception) {
    }
}
