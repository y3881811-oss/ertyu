package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ertyu.engine.CrashMarker
import com.ertyu.model.CrashPromptHolder
import com.ertyu.model.CrashPromptState

/** 上次运行崩溃过：启动时提示一次，可直接去日志页查看/导出 */
@Composable
fun CrashPromptDialog(vm: MainViewModel) {
    val context = LocalContext.current
    val state by CrashPromptHolder.state.collectAsState()

    LaunchedEffect(Unit) {
        val info = CrashMarker.read(context) ?: return@LaunchedEffect
        CrashPromptHolder.state.value = CrashPromptState(
            visible = true,
            timeText = info.timeText,
            summary = info.summary,
            logPath = info.logPath
        )
    }

    if (!state.visible) return

    AlertDialog(
        onDismissRequest = {
            CrashMarker.clear(context)
            CrashPromptHolder.state.value = CrashPromptState()
        },
        title = { Text("上次运行崩溃了") },
        text = {
            Column {
                Text("时间：${state.timeText}", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    state.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "崩溃堆栈已写进日志，可在日志页导出后发给我排查。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                CrashMarker.clear(context)
                CrashPromptHolder.state.value = CrashPromptState()
                vm.openLogViewer()
            }) { Text("查看日志") }
        },
        dismissButton = {
            TextButton(onClick = {
                CrashMarker.clear(context)
                CrashPromptHolder.state.value = CrashPromptState()
            }) { Text("知道了") }
        }
    )
}
