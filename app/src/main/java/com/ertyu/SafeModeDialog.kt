package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.ertyu.model.SafeModeHolder

@Composable
fun SafeModeDialog(vm: MainViewModel) {
    val state by SafeModeHolder.state.collectAsState()
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { vm.dismissSafeModeDialog() },
        title = { Text("安全模式") },
        text = {
            Column {
                Text("检测到最近连续崩溃 ${state.crashCount} 次，App 可能因为某些重负载功能不稳定。")
                Spacer(Modifier.height(8.dp))
                Text(
                    "可以先把耗资源的自动扫描关掉，再观察是否恢复正常；也可以查看日志找原因。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.applySafeModeDowngrade() }) { Text("关闭重负载功能") }
        },
        dismissButton = {
            TextButton(onClick = { vm.openLogViewer(); vm.dismissSafeModeDialog() }) { Text("查看日志") }
        }
    )
}
