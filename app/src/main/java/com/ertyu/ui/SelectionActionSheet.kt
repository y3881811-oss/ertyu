package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem

/**
 * 选择模式下的批量/单项操作菜单。
 * - count == 1：显示单文件操作（重命名、信息、解压、压缩、分享、安装、签名）
 * - count >  1：只显示批量操作（批量重命名、压缩、删除等）
 * 通用项（全选、反选、复制、移动、删除）两者都显示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionActionSheet(
    visible: Boolean,
    selectedItems: List<FileItem>,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onCompress: () -> Unit,
    onBatchRename: () -> Unit,
    // 单文件专属
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

            // ===== 通用项 =====
            SheetItem("全选") { onSelectAll(); onDismiss() }
            SheetItem("反选") { onInvertSelection(); onDismiss() }
            SheetItem("复制") { onCopy(); onDismiss() }
            SheetItem("移动") { onMove(); onDismiss() }
            SheetItem("删除", isDanger = true) { onDelete(); onDismiss() }

            // ===== 单个专属 =====
            if (single != null) {
                onOpenSingle?.let { SheetItem("打开") { it(single); onDismiss() } }
                onRenameSingle?.let { SheetItem("重命名") { it(single); onDismiss() } }
                onPropertiesSingle?.let { SheetItem("属性") { it(single); onDismiss() } }
                onExtractSingle?.let { SheetItem("解压") { it(single); onDismiss() } }
                onCompressSingle?.let { SheetItem("压缩") { it(single); onDismiss() } }
                onShareSingle?.let { SheetItem("分享") { it(single); onDismiss() } }
                onInstallSingle?.let { SheetItem("安装") { it(single); onDismiss() } }
                onSignSingle?.let { SheetItem("签名") { it(single); onDismiss() } }
            } else {
                // ===== 多个专属 =====
                SheetItem("批量重命名") { onBatchRename(); onDismiss() }
                SheetItem("压缩") { onCompress(); onDismiss() }
            }

            Spacer(Modifier.height(16.dp))
        }
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