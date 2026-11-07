package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    hasClipboardItems: Boolean, // 👈 新增：是否有剪贴板内容
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCopyToClipboard: () -> Unit, // 👈 新增
    onCutToClipboard: () -> Unit,  // 👈 新增
    onPaste: () -> Unit,           // 👈 新增
    onCopyToOther: () -> Unit,     // 👈 你原来的直接复制到对侧
    onMoveToOther: () -> Unit,     // 👈 你原来的直接移动到对侧
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

            // 👇 剪贴板功能
            if (hasClipboardItems) {
                SheetItem("粘贴到当前目录") { onPaste(); onDismiss() }
            }
            
            if (selectedItems.isNotEmpty()) {
                SheetItem("复制 (到剪贴板)") { onCopyToClipboard(); onDismiss() }
                SheetItem("剪切 (到剪贴板)") { onCutToClipboard(); onDismiss() }
                
                // 保留你原有的快捷操作
                SheetItem("直接复制到对侧") { onCopyToOther(); onDismiss() }
                SheetItem("直接移动到对侧") { onMoveToOther(); onDismiss() }
            }

            SheetItem("删除", isDanger = true) {
                useTrash = defaultUseTrash
                showDeleteConfirm = true
            }

            if (single != null) {
                SheetItem("重命名") { onBatchRename(); onDismiss() }
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
                        if (selectedItems.size == 1) "确定删除「${selectedItems.firstOrNull()?.name ?: ""}」吗？"
                        else "确定删除选中的 ${selectedItems.size} 项吗？"
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { useTrash = !useTrash }
                    ) {
                        Checkbox(checked = useTrash, onCheckedChange = { useTrash = it })
                        Spacer(Modifier.width(4.dp))
                        Text("移入回收站")
                    }
                    Spacer(Modifier.height(4.dp))
                    if (useTrash) {
                        Text("文件会先放入回收站，可后续恢复", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text("警告：文件将被彻底删除，此操作不可撤销！", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(useTrash)
                    onDismiss()
                }) { Text("确认删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun SheetItem(text: String, isDanger: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}