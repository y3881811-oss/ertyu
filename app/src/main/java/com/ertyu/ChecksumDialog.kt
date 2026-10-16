package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
fun ChecksumDialog(vm: MainViewModel) {
    val state by BatchToolsState.checksumState.collectAsState()
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { vm.closeChecksum() },
        title = { Text("文件校验和") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    state.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "大小：${formatChecksumSize(state.fileSize)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(12.dp))

                if (state.error != null) {
                    Text(
                        "错误：${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                    return@Column
                }

                if (state.isCalculating) {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                }

                HashRow("MD5", state.md5)
                Spacer(Modifier.height(8.dp))
                HashRow("SHA-1", state.sha1)
                Spacer(Modifier.height(8.dp))
                HashRow("SHA-256", state.sha256)
            }
        },
        confirmButton = {
            if (!state.isCalculating) {
                TextButton(onClick = { vm.recalcChecksum() }) {
                    Text("重新计算")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.closeChecksum() }) {
                Text("关闭")
            }
        }
    )
}

@Composable
private fun HashRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value.ifEmpty { "计算中…" },
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatChecksumSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}