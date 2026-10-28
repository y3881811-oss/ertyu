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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ApkInstaller

@Composable
fun DialogInstall(vm: MainViewModel) {
    val context = LocalContext.current
    val installState by vm.installState.collectAsState()
    if (installState.visible) {
        AlertDialog(
            onDismissRequest = { if (installState.error != null) vm.dismissInstallProgress() },
            title = { Text("正在安装") },
            text = {
                Column {
                    Text(installState.fileName, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    if (installState.total > 0) {
                        val progressValue = (installState.copied.toFloat() / installState.total.toFloat()).coerceIn(0f, 1f)
                        LinearProgressIndicator(progress = { if (progressValue.isNaN()) 0f else progressValue }, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("${installState.percent}%")
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(installState.stage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    installState.error?.let { Spacer(Modifier.height(8.dp)); Text("错误: $it", color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { if (installState.error != null) TextButton(onClick = { vm.dismissInstallProgress() }) { Text("关闭") } }
        )
    }

    val pendingInstall by vm.pendingInstall.collectAsState()
    if (pendingInstall != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissInstallPermissionPrompt() },
            title = { Text("需要安装权限") },
            text = { Text("本应用需要「安装未知应用」权限才能安装 APK。\n\n点击「去授权」后，在系统设置里开启本应用的开关，返回后再次点击安装即可。") },
            confirmButton = { TextButton(onClick = { ApkInstaller.openInstallPermissionSettings(context); vm.dismissInstallPermissionPrompt() }) { Text("去授权") } },
            dismissButton = { TextButton(onClick = { vm.dismissInstallPermissionPrompt() }) { Text("取消") } }
        )
    }
}