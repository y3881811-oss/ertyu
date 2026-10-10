package com.ertyu

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem
import com.ertyu.model.PanelState

@Composable
fun FilePanel(
    state: PanelState,
    onOpen: (Uri) -> Unit,
    onUp: () -> Unit,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxHeight()) {
        // 顶部工具栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onUp, enabled = state.canGoUp) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "上级")
            }
            Text(
                text = state.currentUri?.path ?: "未选择目录",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider()

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("错误: ${state.error}")
            }

            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("空目录")
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.items, key = { it.uri.toString() }) { item ->
                    FileRow(item, onOpen, onDelete, onCopy)
                }
            }
        }
    }
}

@Composable
private fun FileRow(
    item: FileItem,
    onOpen: (Uri) -> Unit,
    onDelete: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            if (!item.isDirectory) {
                Text(formatSize(item.size))
            }
        },
        leadingContent = {
            Icon(
                imageVector = if (item.isDirectory) Icons.Default.Folder
                else Icons.Default.InsertDriveFile,
                contentDescription = null
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = { onCopy(item) }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "复制到另一侧")
                }
                IconButton(onClick = { onDelete(item) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除")
                }
            }
        },
        modifier = Modifier.clickable {
            if (item.isDirectory) onOpen(item.uri)
        }
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}