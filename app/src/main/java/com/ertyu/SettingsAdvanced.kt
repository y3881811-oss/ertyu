package com.ertyu

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ertyu.engine.PermissionHelper

@Composable
fun SettingsAdvanced(vm: MainViewModel) {
    val context = LocalContext.current
    val settings by vm.settingsState.collectAsState()

    Spacer(Modifier.height(16.dp))
    SectionTitle("文件引擎")

    var engineMode by remember { mutableStateOf(settings.engineType) }

    val engines = listOf(
        "普通文件引擎（推荐）",
        "Shizuku 引擎（需授权）",
        "Root 引擎（需 Root）"
    )

    engines.forEachIndexed { index, label ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    engineMode = index
                    if (index == 1) {
                        if (!ShizukuManager.isAvailable()) {
                            vm.showToast("请先安装并启动 Shizuku")
                            return@clickable
                        }
                        if (!ShizukuManager.hasPermission()) {
                            ShizukuManager.requestPermission()
                            vm.showToast("请在弹窗中授权")
                            return@clickable
                        }
                    }
                    vm.switchEngine(index)
                }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = engineMode == index,
                onClick = {
                    engineMode = index
                    if (index == 1) {
                        if (!ShizukuManager.isAvailable()) {
                            vm.showToast("请先安装并启动 Shizuku")
                            return@RadioButton
                        }
                        if (!ShizukuManager.hasPermission()) {
                            ShizukuManager.requestPermission()
                            vm.showToast("请在弹窗中授权")
                            return@RadioButton
                        }
                    }
                    vm.switchEngine(index)
                }
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (index == 0) "无权限要求，最稳定"
                    else if (index == 1) "免 Root 访问受限目录 (Android 11+)"
                    else "完全控制文件系统",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && settings.engineType == 0) {
        Spacer(Modifier.height(8.dp))
        if (!PermissionHelper.hasManageExternalStoragePermission()) {
            Button(
                onClick = { PermissionHelper.requestManageExternalStoragePermission(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("前往开启「所有文件访问权限」")
            }
        } else {
            Text(
                "已拥有「所有文件访问权限」",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}