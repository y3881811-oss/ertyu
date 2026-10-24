package com.ertyu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun FileActionDock(
    isLeft: Boolean,
    vm: MainViewModel,
    onScrollToTop: () -> Unit
) {
    // 使用 Surface 包裹，增加一点圆角和阴影，看起来像个悬浮栏
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 6.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 回到顶部
            IconButton(onClick = onScrollToTop) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = "回到顶部",
                    modifier = Modifier.size(22.dp)
                )
            }

            // 新建文件夹
            IconButton(onClick = { vm.openNewFolderDialog(isLeft) }) {
                Icon(
                    Icons.Default.CreateNewFolder,
                    contentDescription = "新建文件夹",
                    modifier = Modifier.size(22.dp)
                )
            }

            // 新建文件
            IconButton(onClick = { vm.openNewFilePrompt(isLeft) }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建文件",
                    modifier = Modifier.size(22.dp)
                )
            }

            // 排序
            IconButton(onClick = { vm.openSortDialog(isLeft) }) {
                Icon(
                    Icons.Default.Sort,
                    contentDescription = "排序",
                    modifier = Modifier.size(22.dp)
                )
            }

            // 刷新
            IconButton(onClick = { vm.refresh(isLeft) }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新",
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}