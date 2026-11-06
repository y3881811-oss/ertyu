package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionSheet(
    visible: Boolean,
    selectedItems: List<FileItem>,
    defaultUseTrash: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onCompress: () -> Unit,
    onBatchRename: () -> Unit
) {
    if (!visible) return

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var useTrash by remember(defaultUseTrash) { mutableStateOf(defaultUseTrash) }
    val single = selectedItems.singleOrNull()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (selectedItems.size == 1) "已选择 1 项" else "已选择 ${selectedItems.size} 项",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            SheetItem("全选") { onSelectAll(); onDismiss() }
            SheetItem("反选") { onInvertSelection(); onDismiss() }
            SheetItem("复制") { onCopy(); onDismiss() }
            SheetItem("移动") { onMove(); onDismiss() }
            SheetItem("删除", isDanger = true) {
                useTrash = defaultUseTrash
                showDeleteConfirm = true
            }

            if (single != null) {
                SheetItem("批量重命名") { onBatchRename(); onDismiss() }
                SheetItem("压缩") { onCompress(); onDismiss() }
            } else {
                SheetItem("批量重命名") { onBatchRename(); onDismiss() }
                SheetItem("压缩") { onCompress(); onDismiss() }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件") },
            text = {
                Column {
                    Text(
                        if (selectedItems.size == 1) {
                            "确定删除「${selectedItems.firstOrNull()?.name ?: ""}」吗？"
                        } else {
                            "确定删除选中的 ${selectedItems.size} 项吗？"
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useTrash = !useTrash }
                    ) {
                        Checkbox(checked = useTrash, onCheckedChange = { useTrash = it })
                        Spacer(Modifier.width(4.dp))
                        Text("移入回收站")
                    }
                    
                    // 👇 强化警告逻辑
                    Spacer(Modifier.height(4.dp))
                    if (useTrash) {
                        Text("文件会先放入回收站，可后续恢复", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("警告：文件将被彻底删除，此操作不可撤销！", 
                            style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(useTrash)
                    onDismiss()
                }) {
                    Text("确认删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SheetItem(text: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}