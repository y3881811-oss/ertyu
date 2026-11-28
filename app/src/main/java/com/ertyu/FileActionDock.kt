package com.ertyu

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.Sort // 👈 修改了导包
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FileActionDock(
    isLeft: Boolean,
    vm: MainViewModel,
    onScrollToTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onScrollToTop) {
                Icon(Icons.Default.VerticalAlignTop, contentDescription = "回到顶部")
            }
            IconButton(onClick = { vm.openNewFolderDialog(isLeft) }) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
            }
            IconButton(onClick = { vm.openNewFilePrompt(isLeft) }) {
                Icon(Icons.AutoMirrored.Filled.NoteAdd, contentDescription = "新建文件")
            }
            IconButton(onClick = { vm.openSortDialog(isLeft) }) {
                // 👇 修改了图标调用
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "排序")
            }
            IconButton(onClick = { vm.openSearch(isLeft) }) {
                Icon(Icons.Default.Search, contentDescription = "搜索")
            }
        }
    }
}