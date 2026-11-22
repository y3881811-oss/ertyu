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
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.UpdateDownloadHolder

@Composable
fun UpdateDownloadDialog(vm: MainViewModel) {
    val state by UpdateDownloadHolder.state.collectAsState()
    if (!state.visible) return

    val finished = state.done || state.error != null
    val progress = when {
        state.total > 0 -> state.downloaded.toFloat() / state.total.toFloat()
        state.percent > 0 -> state.percent / 100f
        else -> 0f
    }.coerceIn(0f, 1f)

    AlertDialog(
        onDismissRequest = { if (finished) vm.dismissUpdateDownload() },
        title = { Text("更新 EYU文件管理器") },
        text = {
            Column {
                Text(state.phase, style = MaterialTheme.typography.bodyMedium)
                if (state.total > 0) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${ItemSizeEngine.format(state.downloaded)} / ${ItemSizeEngine.format(state.total)} · ${state.percent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (state.phase.contains("下载")) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                if (state.done && state.error == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "已启动系统安装界面，按提示完成覆盖安装即可。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            if (finished) {
                TextButton(onClick = { vm.dismissUpdateDownload() }) { Text("关闭") }
            } else {
                TextButton(onClick = { vm.cancelUpdateDownload() }) { Text("取消下载") }
            }
        },
        dismissButton = null
    )
}
