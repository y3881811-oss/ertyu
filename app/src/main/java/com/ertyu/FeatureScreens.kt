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
                    ListItem(
                        headlineText = { Text(res.file.name) },
                        supportingText = { Text("第 ${res.lineNum} 行: ${res.content}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.clickable {
                            vm.openEditor(res.file.absolutePath)
                            vm.closeContentSearch()
                        }
                    )
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
                title = { Text("重复文件清理 (${state.selectedFiles.size})") },
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
                            Text("组 ${index + 1} (${group.files.size} 个文件)", Modifier.padding(16.dp, 8.dp), style = MaterialTheme.typography.titleMedium)
                        }
                        items(group.files) { file ->
                            val isSelected = state.selectedFiles.contains(file.absolutePath)
                            ListItem(
                                headlineText = { Text(file.name) },
                                supportingText = { Text(file.absolutePath, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingContent = {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { vm.toggleDuplicateSelection(file.absolutePath) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}