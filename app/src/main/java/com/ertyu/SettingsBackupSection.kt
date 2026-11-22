package com.ertyu

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.SettingsBackupHolder

@Composable
fun SettingsBackupSection(vm: MainViewModel) {
    val state by SettingsBackupHolder.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.prepareSettingsImport(uri)
        } else {
            vm.showToast("已取消")
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = !state.busy) { vm.exportSettingsBackup() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("备份设置", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "导出为 Download/EYU-settings-时间.json",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(enabled = !state.busy) {
                    picker.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Restore, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("从备份文件恢复", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "选择之前导出的 JSON，恢复前会让你确认",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 4.dp))
    }

    state.pending?.let { payload ->
        val s = payload.settings
        AlertDialog(
            onDismissRequest = { vm.cancelSettingsImport() },
            title = { Text("恢复设置") },
            text = {
                Column {
                    Text("来自：${state.fileName}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("将覆盖当前的以下设置：")
                    Spacer(Modifier.height(4.dp))
                    Text("• 面板模式：${s.panelMode.name}", style = MaterialTheme.typography.bodySmall)
                    Text("• 默认排序：${s.sortModeDefault.name}，网格模式：${if (s.isGridMode) "开" else "关"}", style = MaterialTheme.typography.bodySmall)
                    Text("• 默认显示隐藏文件：${if (s.showHiddenByDefault) "开" else "关"}", style = MaterialTheme.typography.bodySmall)
                    Text("• 使用回收站：${if (s.useRecycleBin) "开" else "关"}", style = MaterialTheme.typography.bodySmall)
                    Text("• 文件引擎：${when (s.engineType) { 1 -> "Shizuku"; 2 -> "Root"; else -> "普通" }}", style = MaterialTheme.typography.bodySmall)
                    Text("• WebDAV：端口 ${s.webDavPort}，用户 ${s.webDavUsername}", style = MaterialTheme.typography.bodySmall)
                    Text("• 自定义路径：${s.customPaths.size} 条", style = MaterialTheme.typography.bodySmall)
                    Text("• 显示文件夹大小：${if (payload.showFolderSize) "开" else "关"}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "现有设置会被替换，且无法撤销。",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { vm.applySettingsImport() }) {
                    Text("覆盖并恢复", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelSettingsImport() }) { Text("取消") }
            }
        )
    }
}
