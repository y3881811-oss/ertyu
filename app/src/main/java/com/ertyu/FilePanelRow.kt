package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

// 👇 补上这个消失的组件
@Composable
fun SelectionToolbar(
    count: Int,
    onClear: () -> Unit,
    onSelectAll: () -> Unit,
    onInvert: () -> Unit,
    onSelectByType: (String) -> Unit,
    onRename: () -> Unit,
    onZip: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onDiff: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClear) {
            Icon(Icons.Default.Close, contentDescription = "取消")
        }
        Text(
            text = "已选 $count 项",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        Box {
            IconButton(onClick = { showMenu = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SelectAll, contentDescription = "选择")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("全选") }, onClick = { showMenu = false; onSelectAll() })
                DropdownMenuItem(text = { Text("反选") }, onClick = { showMenu = false; onInvert() })
                DropdownMenuItem(text = { Text("全选图片") }, onClick = { showMenu = false; onSelectByType("jpg"); onSelectByType("png") })
            }
        }

        IconButton(onClick = onDiff, enabled = count == 2) {
            Text("≠", style = MaterialTheme.typography.titleLarge, color = if (count == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRename) { Icon(Icons.Default.Edit, contentDescription = "批量重命名") }
        IconButton(onClick = onZip) { Icon(Icons.Default.FolderZip, contentDescription = "压缩") }
        IconButton(onClick = onCopy) { Icon(Icons.Default.Add, contentDescription = "复制") }
        IconButton(onClick = onMove) { Icon(Icons.Default.ArrowUpward, contentDescription = "移动") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除") }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    item: FileItem,
    isSelected: () -> Boolean, // 👈 保留 Lambda，不卡顿
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val selected = isSelected()
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    val subtitle = remember(item.uriString, item.sizeText, item.typeText, item.lastModifiedText) {
        buildString {
            append(item.typeText)
            if (!item.isDirectory && item.sizeText.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(item.sizeText)
            }
            if (item.lastModifiedText.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(item.lastModifiedText)
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 12.dp, end = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
            FileThumbnail(item = item)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle.isNotEmpty()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: () -> Boolean, // 👈 保留 Lambda
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val selected = isSelected()
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    Column(
        modifier = Modifier
            .aspectRatio(1f).padding(4.dp).clip(RoundedCornerShape(8.dp)).background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            FileThumbnail(item = item)
        }
        Spacer(Modifier.height(6.dp))
        Text(text = item.name, style = MaterialTheme.typography.bodySmall, maxLines = 2, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }
}