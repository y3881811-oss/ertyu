package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.SplitHolder

private val SIZE_OPTIONS = listOf(10, 50, 100, 200, 500)

@Composable
fun SplitDialog(vm: MainViewModel) {
    val state by SplitHolder.state.collectAsState()
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { vm.closeSplitDialog() },
        title = { Text(if (state.isMerge) "合并分卷" else "分卷") },
        text = {
            Column {
                Text(
                    text = state.targetName,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (state.isMerge) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "会把同名的 .001/.002… 按顺序合并成一个文件。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text("每卷大小", style = MaterialTheme.typography.labelMedium)
                    SIZE_OPTIONS.forEach { mb ->
                        androidx.compose.foundation.layout.Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(selected = state.partSizeMb == mb, enabled = !state.running) {
                                    vm.setSplitPartSize(mb)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.partSizeMb == mb,
                                enabled = !state.running,
                                onClick = { vm.setSplitPartSize(mb) }
                            )
                            Text("$mb MB")
                        }
                    }
                }
                if (state.running) {
                    Spacer(Modifier.height(10.dp))
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(state.message, style = MaterialTheme.typography.labelSmall)
                } else if (state.message.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !state.running, onClick = { vm.confirmSplit() }) {
                Text(if (state.isMerge) "开始合并" else "开始分卷")
            }
        },
        dismissButton = {
            TextButton(enabled = !state.running, onClick = { vm.closeSplitDialog() }) { Text("取消") }
        }
    )
}
