package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ertyu.engine.HiddenExtPrefs
import com.ertyu.model.HiddenExtHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenExtScreen(vm: MainViewModel) {
    val context = LocalContext.current
    val exts by HiddenExtHolder.exts.collectAsState()
    val enabled by HiddenExtHolder.enabled.collectAsState()
    var input by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }

    fun persist() {
        HiddenExtPrefs.save(context)
        vm.refresh(true)
        vm.refresh(false)
    }

    BackHandler { HiddenExtHolder.visible.value = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐藏的文件类型") },
                navigationIcon = {
                    IconButton(onClick = { HiddenExtHolder.visible.value = false }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { confirmClear = true }, enabled = exts.isNotEmpty()) {
                        Text("全部清除", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("启用过滤")
                    Text(
                        "开启后，这些扩展名的文件不会出现在列表里（文件夹不受影响）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        HiddenExtHolder.enabled.value = it
                        persist()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text("扩展名，多个用逗号或空格分隔") },
                    placeholder = { Text("apk, log, tmp") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(onClick = {
                    val adds = input.split(',', ' ', '、')
                        .map { HiddenExtHolder.normalize(it) }
                        .filter { it.isNotEmpty() }
                        .toSet()
                    if (adds.isNotEmpty()) {
                        HiddenExtHolder.exts.value = exts + adds
                        input = ""
                        persist()
                        vm.showToast("已隐藏 ${adds.size} 种类型")
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }

            Spacer(Modifier.height(6.dp))

            // 常用预设
            val presets = listOf("apk", "log", "tmp", "db", "so", "zip", "jpg", "mp4")
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.take(4).forEach { p ->
                    AssistChip(
                        onClick = {
                            HiddenExtHolder.exts.value = exts + p
                            persist()
                        },
                        enabled = p !in exts,
                        label = { Text(".$p") }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.drop(4).forEach { p ->
                    AssistChip(
                        onClick = {
                            HiddenExtHolder.exts.value = exts + p
                            persist()
                        },
                        enabled = p !in exts,
                        label = { Text(".$p") }
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 6.dp))

            if (exts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "还没有隐藏任何类型",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(exts.sorted()) { ext ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ".$ext",
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                HiddenExtHolder.exts.value = exts - ext
                                persist()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "移除")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清除全部隐藏类型") },
            text = { Text("清除后所有文件都会重新显示，设置本身不会影响任何文件。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    HiddenExtHolder.exts.value = emptySet()
                    persist()
                }) { Text("清除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }
}
