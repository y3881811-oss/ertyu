package com.ertyu

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List // 👈 修改了导包
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun FileToolbar(
    isLeft: Boolean,
    isCompact: Boolean,
    vm: MainViewModel,
    isGridMode: Boolean,
    onToggleGrid: () -> Unit,
    path: String,
    canGoUp: Boolean,
    showHidden: Boolean,
    showSwitch: Boolean,
    onSwitchPanel: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isCompact) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "菜单")
            }
        }

        IconButton(onClick = { vm.goUp(isLeft) }, enabled = canGoUp) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "上级目录")
        }

        Text(
            text = path,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        )

        IconButton(onClick = onToggleGrid) {
            Icon(
                // 👇 修改了图标调用
                imageVector = if (isGridMode) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                contentDescription = if (isGridMode) "列表视图" else "网格视图"
            )
        }

        IconButton(onClick = { vm.toggleHidden(isLeft) }) {
            Icon(
                imageVector = if (showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = "显示隐藏文件"
            )
        }

        if (showSwitch) {
            IconButton(onClick = onSwitchPanel) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "切换面板")
            }
        }
    }
}