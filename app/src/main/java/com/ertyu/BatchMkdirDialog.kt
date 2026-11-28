package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.BatchMkdirHolder

@Composable
fun BatchMkdirDialog(vm: MainViewModel) {
    val state by BatchMkdirHolder.state.collectAsState()
    if (!state.visible) return

    var text by remember { mutableStateOf("") }
    val names = remember(text) {
        text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
    }

    AlertDialog(
        onDismissRequest = { if (!state.creating) vm.closeBatchMkdir() },
        title = { Text("批量新建文件夹") },
        text = {
            Column {
                Text(
                    "每行一个文件夹名，同名会创建失败并被跳过。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("相册\n工作\n假期照片") },
                    minLines = 5,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "共 ${names.size} 个" + if (names.toSet().size != names.size) "（有重复，会自动去重）" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.creating) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.createBatchFolders(names) },
                enabled = names.isNotEmpty() && !state.creating
            ) { Text("创建 ${names.toSet().size} 个") }
        },
        dismissButton = {
            TextButton(onClick = { vm.closeBatchMkdir() }, enabled = !state.creating) { Text("取消") }
        }
    )
}
