package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentSearchScreen(vm: MainViewModel, rootDir: java.io.File) {
    val state by vm.contentSearchState.collectAsState()
    var keyword by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        placeholder = { Text("搜索文件内容...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeContentSearch() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { vm.startContentSearch(rootDir, keyword) }) {
                        Text("搜索")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isSearching) LinearProgressIndicator(Modifier.fillMaxWidth())
            LazyColumn {
                items(state.results) { res ->
                    // 👇 改用 Row + Column 手写列表项，兼容性无敌
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // TODO: 如果你需要打开编辑器，请在这里改成你 ViewModel 里实际的方法名
                                // vm.openEditor(res.file.absolutePath)
                                vm.showToast("点击了: ${res.file.name}")
                                vm.closeContentSearch()
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            text = res.file.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "第 ${res.lineNum} 行: ${res.content}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateScanScreen(vm: MainViewModel, rootDir: java.io.File) {
    val state by vm.duplicateScanState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("重复文件清理 (已选 ${state.selectedFiles.size})") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeDuplicateScan() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.deleteSelectedDuplicates() }) {
                        Icon(Icons.Default.Delete, "删除选中")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.isScanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(state.progressText, Modifier.padding(16.dp))
            } else if (state.groups.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Button(onClick = { vm.startDuplicateScan(rootDir) }) {
                        Text("开始扫描重复文件")
                    }
                }
            } else {
                LazyColumn {
                    state.groups.forEachIndexed { index, group ->
                        item {
                            Text(
                                "组 ${index + 1} (${group.files.size} 个文件)",
                                Modifier.padding(16.dp, 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        items(group.files) { file ->
                            val isSelected = state.selectedFiles.contains(file.absolutePath)
                            
                            // 👇 同样手写 Row，彻底摆脱 ListItem 依赖
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { vm.toggleDuplicateSelection(file.absolutePath) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { vm.toggleDuplicateSelection(file.absolutePath) }
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(file.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        file.absolutePath,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}