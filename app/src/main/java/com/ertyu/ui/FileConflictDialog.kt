package com.ertyu.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.ConflictAction
import com.ertyu.model.FileConflictState

@Composable
fun FileConflictDialog(
    state: FileConflictState,
    onDismiss: () -> Unit
) {
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件冲突") },
        text = {
            Column {
                Text("目标目录中已存在 ${state.conflictingNames.size} 个同名文件。")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "例如: ${state.conflictingNames.take(3).joinToString(", ")}" + 
                           if (state.conflictingNames.size > 3) " 等" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("请选择如何处理这些文件：")
            }
        },
        confirmButton = {
            TextButton(onClick = { 
                state.onResolve?.invoke(ConflictAction.KEEP_BOTH)
                onDismiss()
            }) {
                Text("保留两者 (重命名)")
            }
        },
        dismissButton = {
            Column {
                TextButton(onClick = { 
                    state.onResolve?.invoke(ConflictAction.OVERWRITE)
                    onDismiss()
                }) {
                    Text("覆盖", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { 
                    state.onResolve?.invoke(ConflictAction.SKIP)
                    onDismiss()
                }) {
                    Text("跳过")
                }
                TextButton(onClick = { 
                    state.onResolve?.invoke(ConflictAction.CANCEL)
                    onDismiss()
                }) {
                    Text("取消全部")
                }
            }
        }
    )
}