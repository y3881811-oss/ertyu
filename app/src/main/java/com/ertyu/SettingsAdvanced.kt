package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsAdvanced(vm: MainViewModel) {
    Spacer(Modifier.height(16.dp))
    SectionTitle("高级引擎")
    var useShizuku by remember { mutableStateOf(false) }
    var shizukuStatus by remember { mutableStateOf("未授权") }

    LaunchedEffect(Unit) {
        if (ShizukuManager.isAvailable()) {
            shizukuStatus = if (ShizukuManager.hasPermission()) "已授权" else "需要授权"
        } else {
            shizukuStatus = "服务未运行"
        }
    }

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("使用 Shizuku 引擎", style = MaterialTheme.typography.bodyLarge)
            Text("访问 Android/data 等受限目录 (状态: $shizukuStatus)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = useShizuku,
            onCheckedChange = { checked ->
                if (checked) {
                    if (!ShizukuManager.isAvailable()) { vm.showToast("请先安装并启动 Shizuku"); return@Switch }
                    if (!ShizukuManager.hasPermission()) { ShizukuManager.requestPermission(); vm.showToast("请在弹窗中授权") }
                    else { useShizuku = true; vm.switchEngine(useShizuku = true) }
                } else { useShizuku = false; vm.switchEngine(useShizuku = false) }
            }
        )
    }
}