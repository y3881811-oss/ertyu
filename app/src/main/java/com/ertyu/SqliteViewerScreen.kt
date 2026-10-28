package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SqliteViewerScreen(vm: MainViewModel) {
    val state by SqliteStateHolder.state.collectAsState()

    BackHandler {
        if (state.selectedTable != null) vm.closeSqliteTable()
        else vm.closeSqlite()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedTable ?: "数据库: ${state.dbName}") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedTable != null) vm.closeSqliteTable()
                        else vm.closeSqlite()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text("错误: ${state.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                state.selectedTable == null -> {
                    // 显示表列表
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.tables) { table ->
                            Text(
                                text = table,
                                modifier = Modifier.fillMaxWidth().clickable { vm.selectSqliteTable(table) }.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            HorizontalDivider()
                        }
                    }
                }
                else -> {
                    // 显示表数据
                    val hScroll = rememberScrollState()
                    Column(Modifier.fillMaxSize().horizontalScroll(hScroll)) {
                        // 表头
                        Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)) {
                            state.columns.forEach { col ->
                                Text(col, modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                            }
                        }
                        HorizontalDivider()
                        // 数据行
                        LazyColumn {
                            items(state.rows) { row ->
                                Row(Modifier.padding(8.dp)) {
                                    row.forEach { cell ->
                                        Text(cell, modifier = Modifier.padding(end = 16.dp), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                }
                                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            }
        }
    }
}