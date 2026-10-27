package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ApkEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkBrowserScreen(vm: MainViewModel) {
    val state by vm.apkBrowserState.collectAsState()
    var entryActionTarget by remember { mutableStateOf<ApkEntry?>(null) }
    // 内部层级状态，建议放入 ViewModel，这里为了简单先用局部状态
    var currentDir by remember { mutableStateOf("") }

    val displayEntries = remember(state.entries, state.filterQuery, currentDir) {
        if (state.filterQuery.isNotBlank()) {
            state.entries.filter { it.path.contains(state.filterQuery, ignoreCase = true) }
        } else {
            val subEntries = state.entries.filter { it.path.startsWith(currentDir) && it.path != currentDir }
            val folders = mutableSetOf<String>()
            val files = mutableListOf<ApkEntry>()
            subEntries.forEach {
                val rel = it.path.substring(currentDir.length)
                if (rel.contains("/")) folders.add(rel.substringBefore("/"))
                else files.add(it)
            }
            val res = mutableListOf<ApkEntry>()
            folders.sorted().forEach {
                res.add(ApkEntry(path = "$currentDir$it/", name = it, isDirectory = true, size = 0, compressedSize = 0, sizeText = "", compressedText = "", depth = 0, isDex = false))
            }
            res.addAll(files.sortedBy { it.name.lowercase() })
            res
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.apkName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("/$currentDir", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeApkBrowser() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.filterQuery,
                onValueChange = { vm.updateApkBrowserFilter(it) },
                placeholder = { Text("过滤路径…") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (state.filterQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.updateApkBrowserFilter("") }) { Icon(Icons.Default.Close, null) }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            HorizontalDivider()

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("错误: ${state.error}", color = MaterialTheme.colorScheme.error) }
                else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(vertical = 4.dp)) {
                    if (state.filterQuery.isEmpty() && currentDir.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { 
                                    val parent = currentDir.trimEnd('/').substringBeforeLast('/', "")
                                    currentDir = if (parent.isEmpty()) "" else "$parent/"
                                }.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = "..", tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text("..", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    items(displayEntries, key = { it.path }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Transparent)
                                .clickable {
                                    if (entry.isDirectory) {
                                        currentDir = entry.path
                                    } else {
                                        // 如果是 dex 文件，直接打开 DEX 查看器
                                        if (entry.name.endsWith(".dex")) {
                                            vm.openDexViewerFromApk(state.apkPath, entry.path)
                                        } else {
                                            entryActionTarget = entry
                                        }
                                    }
                                }
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
                                Text(entry.name, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (!entry.isDirectory) {
                                    Text(
                                        text = "${entry.sizeText} · 压缩 ${entry.compressedText}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    entryActionTarget?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryActionTarget = null },
            title = { Text(entry.path, maxLines = 3, overflow = TextOverflow.Ellipsis) },
            text = { Column { Text("大小：${entry.sizeText}") } },
            confirmButton = {
                TextButton(onClick = {
                    vm.extractApkEntry(entry)
                    entryActionTarget = null
                }) { Text("提取到当前目录") }
            },
            dismissButton = {
                TextButton(onClick = { entryActionTarget = null }) { Text("取消") }
            }
        )
    }
}