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
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.FileItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionSheet(
    visible: Boolean,
    selectedItems: List<FileItem>,
    defaultUseTrash: Boolean,
    hasClipboardItems: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCopyToClipboard: () -> Unit,
    onCutToClipboard: () -> Unit,
    onPaste: () -> Unit,
    onCopyToOther: () -> Unit,
    onMoveToOther: () -> Unit,
    onDelete: (Boolean) -> Unit,
    onCompress: (List<FileItem>) -> Unit,         // 👈 改成传 List
    onBatchRename: () -> Unit,
    onInstallApk: ((FileItem) -> Unit)? = null,
    onBrowseApk: ((FileItem) -> Unit)? = null,
    onExtractArchive: ((List<FileItem>) -> Unit)? = null, // 👈 改成传 List
    onBatchChecksum: ((List<FileItem>) -> Unit)? = null,
    onBatchInstall: ((List<FileItem>) -> Unit)? = null,
    onViewManifest: ((FileItem) -> Unit)? = null
) {
    if (!visible) return

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var useTrash by remember(defaultUseTrash) { mutableStateOf(defaultUseTrash) }
    
    val isSingle = selectedItems.size == 1
    val singleItem = selectedItems.firstOrNull()
    
    // 判断是否选中了单个 APK 文件
    val isApk = isSingle && singleItem?.name?.endsWith(".apk", ignoreCase = true) == true
    // 判断是否选中了压缩包（单个或多个都支持）
    val hasArchive = selectedItems.any { ArchiveFormat.isArchive(it.name) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = if (isSingle) "已选择 1 项" else "已选择 ${selectedItems.size} 项",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))

            // ================== 1. 专属操作 ==================
            if (isApk) {
                onInstallApk?.let {
                    SheetItem("安装应用", isPrimary = true) { it(singleItem!!); onDismiss() }
                }
                onBrowseApk?.let {
                    SheetItem("浏览内部", isPrimary = true) { it(singleItem!!); onDismiss() }
                }
                onViewManifest?.let {
                    SheetItem("查看 Manifest / 权限") { it(singleItem!!); onDismiss() }
                }
                Spacer(Modifier.height(8.dp))
            } else if (hasArchive) {
                // 👇 只要有压缩包，就显示解压
                onExtractArchive?.let {
                    SheetItem("解压文件", isPrimary = true) { it(selectedItems); onDismiss() }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ================== 2. 通用操作 ==================
            SheetItem("全选") { onSelectAll(); onDismiss() }
            SheetItem("反选") { onInvertSelection(); onDismiss() }
            
            SheetItem("复制 (到剪贴板)") { onCopyToClipboard(); onDismiss() }
            SheetItem("剪切 (到剪贴板)") { onCutToClipboard(); onDismiss() }
            
            if (hasClipboardItems) {
                SheetItem("粘贴到当前目录", isPrimary = true) { onPaste(); onDismiss() }
            }
            
            SheetItem("直接复制到对侧") { onCopyToOther(); onDismiss() }
            SheetItem("直接移动到对侧") { onMoveToOther(); onDismiss() }
            
            SheetItem("删除", isDanger = true) {
                useTrash = defaultUseTrash
                showDeleteConfirm = true
            }

            // ================== 3. 重命名与压缩 ==================
            if (selectedItems.isNotEmpty()) {
                if (isSingle) {
                    SheetItem("重命名") { onBatchRename(); onDismiss() }
                    SheetItem("压缩") { onCompress(selectedItems); onDismiss() }
                } else {
                    SheetItem("批量重命名") { onBatchRename(); onDismiss() }
                    SheetItem("打包压缩") { onCompress(selectedItems); onDismiss() }
                }
                onBatchChecksum?.let { cb ->
                    SheetItem("校验哈希 (SHA-256)") { cb(selectedItems); onDismiss() }
                }
                if (selectedItems.size > 1 && selectedItems.all { it.name.endsWith(".apk", ignoreCase = true) }) {
                    onBatchInstall?.let { cb ->
                        SheetItem("批量安装 (${selectedItems.size})", isPrimary = true) {
                            cb(selectedItems); onDismiss()
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ================== 删除确认弹窗 ==================
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除文件") },
            text = {
                Column {
                    Text(
                        if (isSingle) "确定删除「${singleItem?.name ?: ""}」吗？"
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
private fun SheetItem(text: String, isDanger: Boolean = false, isPrimary: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = when {
                isDanger -> MaterialTheme.colorScheme.error
                isPrimary -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isPrimary) FontWeight.Bold else FontWeight.Normal
        )
    }
}