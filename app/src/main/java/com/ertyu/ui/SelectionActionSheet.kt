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
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

/**
 * 选择模式长按已选文件弹出的操作菜单。
 * - 选中 1 项：单文件操作 + 通用操作
 * - 选中 N 项：批量操作 + 通用操作
 * 删除操作会先弹二次确认，确认框内可勾选“移入回收站”。
 *
 * @param defaultUseTrash 删除确认框里“移入回收站”的初始勾选状态，
 *                        建议传当前全局设置 settings.useRecycleBin。
 * @param onDelete        参数为本次是否移入回收站。
 */
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
    onBatchRename: () -> Unit,
    onOpenSingle: ((FileItem) -> Unit)? = null,
    onRenameSingle: ((FileItem) -> Unit)? = null,
    onPropertiesSingle: ((FileItem) -> Unit)? = null,
    onExtractSingle: ((FileItem) -> Unit)? = null,
    onCompressSingle: ((FileItem) -> Unit)? = null,
    onShareSingle: ((FileItem) -> Unit)? = null,
    onInstallSingle: ((FileItem) -> Unit)? = null,
    onSignSingle: ((FileItem) -> Unit)? = null
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
                text = if (selectedItems.size == 1) "已选择 1 项"
                       else "已选择 ${selectedItems.size} 项",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            // 通用项
            SheetItem("全选") { onSelectAll(); onDismiss() }
            SheetItem("反选") { onInvertSelection(); onDismiss() }
            SheetItem("复制") { onCopy(); onDismiss() }
            SheetItem("移动") { onMove(); onDismiss() }
            SheetItem("删除", isDanger = true) {
                // 不直接删，先弹二次确认
                useTrash = defaultUseTrash
                showDeleteConfirm = true
            }

            if (single != null) {
                // 单文件专属
                onOpenSingle?.let { SheetItem("打开") { it(single); onDismiss() } }
                onRenameSingle?.let { SheetItem("重命名") { it(single); onDismiss() } }
                onPropertiesSingle?.let { SheetItem("属性") { it(single); onDismiss() } }
                onExtractSingle?.let { SheetItem("解压") { it(single); onDismiss() } }
                onCompressSingle?.let { SheetItem("压缩") { it(single); onDismiss() } }
                onShareSingle?.let { SheetItem("分享") { it(single); onDismiss() } }
                onInstallSingle?.let { SheetItem("安装") { it(single); onDismiss() } }
                onSignSingle?.let { SheetItem("签名") { it(single); onDismiss() } }
            } else {
                // 多选专属
                SheetItem("批量重命名") { onBatchRename(); onDismiss() }
                SheetItem("压缩") { onCompress(); onDismiss() }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // 删除二次确认
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件") },
            text = {
                Column {
                    Text(
                        if (selectedItems.size == 1) {
                            "确定删除「${selectedItems.firstOrNull()?.name ?: ""}」吗？此操作不可撤销。"
                        } else {
                            "确定删除选中的 ${selectedItems.size} 项吗？此操作不可撤销。"
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { useTrash = !useTrash }
                    ) {
                        Checkbox(
                            checked = useTrash,
                            onCheckedChange = { useTrash = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("移入回收站")
                    }
                    if (useTrash) {
                        Text(
                            "文件会先放入回收站，可后续恢复",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete(useTrash)
                    onDismiss()
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun SheetItem(
    text: String,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDanger) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurface
        )
    }
}