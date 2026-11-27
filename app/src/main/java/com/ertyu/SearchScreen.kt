package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ertyu.engine.SearchHistoryStore
import com.ertyu.model.FileItem
import com.ertyu.model.FileKindFilter
import com.ertyu.model.SearchFilter
import com.ertyu.model.SearchHistoryHolder

private val IconFolderSearch: ImageVector = Icons.Default.Folder
private val IconFileSearch: ImageVector = Icons.AutoMirrored.Filled.InsertDriveFile

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(vm: MainViewModel) {
    val state by vm.searchState.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { vm.updateSearchQuery(it) },
                        placeholder = { Text("搜索文件名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { vm.startSearch() })
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeSearch() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.isSearching) {
                        IconButton(onClick = { vm.cancelSearch() }) {
                            Icon(Icons.Default.Close, contentDescription = "取消")
                        }
                    } else {
                        IconButton(onClick = { vm.startSearch() }) {
                            Icon(Icons.Default.Search, contentDescription = "搜索")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusBar(
                searching = state.isSearching,
                dirs = state.scannedDirs,
                files = state.scannedFiles,
                resultCount = state.results.size,
                truncated = state.truncated,
                elapsedMs = state.elapsedMs
            )
            SearchFilterBar(vm, state.filter)
            HorizontalDivider()

            when {
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("错误: ${state.error}")
                }
                state.query.isBlank() -> SearchHistoryBlock(vm)
                state.results.isEmpty() && !state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.filter.isDefault) "未找到匹配的文件" else "没有符合过滤条件的结果",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = state.results,
                        key = { it.uriString },
                        contentType = { if (it.isDirectory) "dir" else "file" }
                    ) { item ->
                        SearchResultRow(
                            item = item,
                            query = state.query,
                            onClick = { vm.handleSearchResultClick(item) },
                            onLongClick = { vm.revealInPanel(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchFilterBar(vm: MainViewModel, filter: SearchFilter) {
    var showSizeMenu by remember { mutableStateOf(false) }
    var showTimeMenu by remember { mutableStateOf(false) }
    val highlightColor = MaterialTheme.colorScheme.primary

    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FileKindFilter.values().forEach { kind ->
                FilterChip(
                    selected = filter.kind == kind,
                    onClick = { vm.setSearchFilter(filter.copy(kind = kind)) },
                    label = { Text(kind.label) }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                AssistChip(
                    onClick = { showSizeMenu = true },
                    label = { Text(if (filter.minSizeMb == 0) "大小不限" else ">${filter.minSizeMb}MB") }
                )
                DropdownMenu(expanded = showSizeMenu, onDismissRequest = { showSizeMenu = false }) {
                    listOf(0, 1, 10, 100, 1024).forEach { mb ->
                        DropdownMenuItem(
                            text = { Text(if (mb == 0) "不限" else if (mb >= 1024) ">1GB" else ">${mb}MB") },
                            onClick = {
                                showSizeMenu = false
                                vm.setSearchFilter(filter.copy(minSizeMb = mb))
                            }
                        )
                    }
                }
            }
            Box {
                AssistChip(
                    onClick = { showTimeMenu = true },
                    label = { Text(if (filter.modifiedWithinDays == 0) "时间不限" else "近 ${filter.modifiedWithinDays} 天") }
                )
                DropdownMenu(expanded = showTimeMenu, onDismissRequest = { showTimeMenu = false }) {
                    listOf(0, 1, 7, 30, 365).forEach { days ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (days) {
                                        0 -> "不限"
                                        1 -> "今天"
                                        365 -> "近 1 年"
                                        else -> "近 $days 天"
                                    }
                                )
                            },
                            onClick = {
                                showTimeMenu = false
                                vm.setSearchFilter(filter.copy(modifiedWithinDays = days))
                            }
                        )
                    }
                }
            }
            if (!filter.isDefault) {
                TextButton(onClick = { vm.setSearchFilter(SearchFilter()) }) {
                    Text("重置", color = highlightColor)
                }
            }
        }
    }
}

@Composable
private fun StatusBar(
    searching: Boolean,
    dirs: Int,
    files: Int,
    resultCount: Int,
    truncated: Boolean,
    elapsedMs: Long
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (searching) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        val text = buildString {
            append("扫描 $dirs 个目录 · $files 个文件")
            append(" · 结果 ")
            append(resultCount)
            if (truncated) append("（已达上限）")
            if (!searching && elapsedMs > 0) {
                append(" · 耗时 ")
                append(String.format(java.util.Locale.ROOT, "%.1f", elapsedMs / 1000.0))
                append("s")
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SearchResultRow(
    item: FileItem,
    query: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) IconFolderSearch else IconFileSearch,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = highlight(item.name, query),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.path,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 把关键词标黄加粗 */
private fun highlight(text: String, query: String) = buildAnnotatedString {
    if (query.isBlank()) {
        append(text)
        return@buildAnnotatedString
    }
    val lowerText = text.lowercase()
    val lowerQuery = query.lowercase()
    var index = 0
    while (true) {
        val hit = lowerText.indexOf(lowerQuery, index)
        if (hit < 0) {
            append(text.substring(index))
            break
        }
        append(text.substring(index, hit))
        withStyle(SpanStyle(background = Color(0xFFFFE082), fontWeight = FontWeight.Bold, color = Color(0xFF3E2723))) {
            append(text.substring(hit, hit + query.length))
        }
        index = hit + query.length
    }
}


@Composable
private fun SearchHistoryBlock(vm: MainViewModel) {
    val context = LocalContext.current
    val items by SearchHistoryHolder.items.collectAsState()

    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "输入关键词开始搜索",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "最近搜索",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { SearchHistoryStore.clear(context) }) { Text("清空") }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(items) { q ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.updateSearchQuery(q)
                            vm.startSearch()
                        }
                        .padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        q,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { SearchHistoryStore.remove(context, q) }) {
                        Icon(Icons.Default.Close, contentDescription = "删除这条历史")
                    }
                }
                HorizontalDivider()
            }
        }
    }
}
