package com.ertyu

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier

@Composable
fun EditorDialogs(
    vm: MainViewModel,
    showDiscardDialog: Boolean,
    onDiscardDismiss: () -> Unit,
    jumpLineText: String,
    onJumpLineTextChange: (String) -> Unit,
    onJumpApplied: (Int) -> Unit
) {
    val state by vm.editor.collectAsState()

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = onDiscardDismiss,
            title = { Text("放弃修改？") },
            text = { Text("文件有未保存的修改，确定要退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDiscardDismiss()
                    vm.closeEditor()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = onDiscardDismiss) {
                    Text("取消")
                }
            }
        )
    }

    if (state.jumpDialogVisible) {
        AlertDialog(
            onDismissRequest = { vm.closeJumpDialog() },
            title = { Text("跳转到行") },
            text = {
                OutlinedTextField(
                    value = jumpLineText,
                    onValueChange = { input ->
                        onJumpLineTextChange(
                            input.filter { c -> c.isDigit() }
                        )
                    },
                    placeholder = {
                        Text("输入行号（1-${state.totalLines}）")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val line = jumpLineText.toIntOrNull()
                    if (line != null && line >= 1 && line <= state.totalLines) {
                        onJumpApplied(line)
                    }
                    onJumpLineTextChange("")
                    vm.closeJumpDialog()
                }) { Text("跳转") }
            },
            dismissButton = {
                TextButton(onClick = {
                    onJumpLineTextChange("")
                    vm.closeJumpDialog()
                }) { Text("取消") }
            }
        )
    }
}