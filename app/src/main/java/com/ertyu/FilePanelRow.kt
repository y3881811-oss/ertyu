package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
        
        // 进阶多选下拉菜单
        Box {
            IconButton(onClick = { showMenu = true }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SelectAll, contentDescription = "选择")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("全选") },
                    onClick = { showMenu = false; onSelectAll() }
                )
                DropdownMenuItem(
                    text = { Text("反选") },
                    onClick = { showMenu = false; onInvert() }
                )
                DropdownMenuItem(
                    text = { Text("全选图片") },
                    onClick = { showMenu = false; onSelectByType("jpg"); onSelectByType("png"); onSelectByType("jpeg"); onSelectByType("webp") }
                )
                DropdownMenuItem(
                    text = { Text("全选视频") },
                    onClick = { showMenu = false; onSelectByType("mp4"); onSelectByType("mkv"); onSelectByType("avi") }
                )
                DropdownMenuItem(
                    text = { Text("全选文本") },
                    onClick = { showMenu = false; onSelectByType("txt"); onSelectByType("md"); onSelectByType("json") }
                )
            }
        }

        IconButton(onClick = onDiff, enabled = count == 2) {
            Text(
                "≠",
                style = MaterialTheme.typography.titleLarge,
                color = if (count == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
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
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

    val subtitle = remember(item.uriString, item.sizeText, item.typeText, item.lastModifiedText) {
        buildString {
            append(item.typeText)
            if (!item.isDirectory && item.sizeText.isNotEmpty()) {
                append(" · ")
                append(item.sizeText)
            }
            if (item.lastModifiedText.isNotEmpty()) {
                append(" · ")
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
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!isSelectionMode) {
            IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
        }
    }
}

// 新增：网格视图 Item
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileGridItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    
    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            FileThumbnail(item = item, modifier = Modifier.fillMaxSize())
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}