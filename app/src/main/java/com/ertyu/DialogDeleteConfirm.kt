package com.ertyu

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun DialogDeleteConfirm(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    val deleteTarget by vm.deleteTarget.collectAsState()
    
    if (deleteTarget != null) {
        val item = deleteTarget!!
        val canUseTrash = settings.useRecycleBin
        AlertDialog(
            onDismissRequest = { vm.dismissDeleteDialog() },
            title = { Text("删除确认") },
            text = { Text("确定要删除「${item.name}」吗？") },
            confirmButton = {
                if (canUseTrash) {
                    TextButton(onClick = { vm.confirmDelete(useTrash = true) }) { Text("放入回收站") }
                } else {
                    TextButton(onClick = { vm.confirmDelete(useTrash = false) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            },
            dismissButton = {
                if (canUseTrash) {
                    TextButton(onClick = { vm.confirmDelete(useTrash = false) }) { Text("直接删除", color = MaterialTheme.colorScheme.error) }
                } else {
                    TextButton(onClick = { vm.dismissDeleteDialog() }) { Text("取消") }
                }
            }
        )
    }
}