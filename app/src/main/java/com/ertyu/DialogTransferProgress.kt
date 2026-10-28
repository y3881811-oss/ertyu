package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DialogTransferProgress(vm: MainViewModel) {
    val state by vm.fileTransferState.collectAsState()

    if (state.visible) {
        AlertDialog(
            onDismissRequest = {
                // 只有完成或失败时才能关闭
                if (state.isDone || state.error != null) vm.dismissTransferDialog()
            },
            title = { Text(state.title) },
            text = {
                Column {
                    Text(
                        state.currentFile,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    
                    if (state.totalBytes > 0) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        // 大小与速度
                        Text(
                            text = "${formatBytes(state.copiedBytes)} / ${formatBytes(state.totalBytes)}  (${state.speedText})",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("错误: $it", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (state.isDone || state.error != null) {
                    TextButton(onClick = { vm.dismissTransferDialog() }) {
                        Text(if (state.error != null) "关闭" else "完成")
                    }
                }
            }
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}