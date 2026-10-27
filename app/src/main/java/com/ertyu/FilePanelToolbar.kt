package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SwapHoriz
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
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .padding(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isCompact) {
            IconButton(onClick = onOpenDrawer) {
                Icon(Icons.Default.Menu, contentDescription = "菜单")
            }
        }
        
        IconButton(onClick = { vm.goUp(isLeft) }, enabled = canGoUp) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "上级")
        }
        
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable { vm.openJumpDialog(isLeft) }
        )
        
        if (showSwitch) {
            IconButton(onClick = onSwitchPanel) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "切换")
            }
        }
        
        if (!isCompact) {
            IconButton(onClick = onToggleGrid) {
                Icon(if (isGridMode) Icons.Default.List else Icons.Default.GridView, contentDescription = "切换视图")
            }
        }
        
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("搜索") },
                    onClick = { menuExpanded = false; vm.openSearch(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("添加书签") },
                    onClick = { menuExpanded = false; vm.addBookmark(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("新建文件夹") },
                    onClick = { menuExpanded = false; vm.openNewFolderDialog(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("新建文件") },
                    onClick = { menuExpanded = false; vm.openNewFilePrompt(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text("排序方式") },
                    onClick = { menuExpanded = false; vm.openSortDialog(isLeft) }
                )
                DropdownMenuItem(
                    text = { Text(if (showHidden) "隐藏隐藏文件" else "显示隐藏文件") },
                    onClick = { menuExpanded = false; vm.setShowHidden(isLeft, !showHidden) }
                )
                DropdownMenuItem(
                    text = { Text("刷新") },
                    onClick = { menuExpanded = false; vm.refresh(isLeft) }
                )
                
                if (isCompact) {
                    DropdownMenuItem(
                        text = { Text("切换网格/列表") },
                        onClick = { menuExpanded = false; onToggleGrid() }
                    )
                }
                DropdownMenuItem(
                    text = { Text("设置") },
                    onClick = { menuExpanded = false; vm.openSettings() }
                )
                DropdownMenuItem(
                    text = { Text("回收站") },
                    onClick = { menuExpanded = false; vm.openRecycleBin() }
                )
            }
        }
    }
}