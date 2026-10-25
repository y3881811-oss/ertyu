package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.model.PanelMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeSettings() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionTitle("面板模式")
            PanelMode.entries.forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { vm.setPanelMode(mode) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.panelMode == mode,
                        onClick = { vm.setPanelMode(mode) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = when (mode) {
                                PanelMode.AUTO -> "自动"
                                PanelMode.DUAL -> "双面板"
                                PanelMode.SINGLE -> "单面板"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            Spacer(Modifier.height(16.dp))
            SectionTitle("高级引擎")
            var useShizuku by remember { mutableStateOf(false) }
            var shizukuStatus by remember { mutableStateOf("未授权") }

            LaunchedEffect(Unit) {
                if (ShizukuManager.isAvailable()) {
                    if (ShizukuManager.hasPermission()) {
                        shizukuStatus = "已授权"
                    } else {
                        shizukuStatus = "需要授权"
                    }
                } else {
                    shizukuStatus = "服务未运行"
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("使用 Shizuku 引擎", style = MaterialTheme.typography.bodyLarge)
                    Text("访问 Android/data 等受限目录 (状态: $shizukuStatus)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = useShizuku,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (!ShizukuManager.isAvailable()) {
                                vm.showToast("请先安装并启动 Shizuku")
                                return@Switch
                            }
                            if (!ShizukuManager.hasPermission()) {
                                ShizukuManager.requestPermission()
                                vm.showToast("请在弹窗中授权")
                            } else {
                                useShizuku = true
                                vm.switchEngine(useShizuku = true)
                            }
                        } else {
                            useShizuku = false
                            vm.switchEngine(useShizuku = false)
                        }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            Spacer(Modifier.height(16.dp))
            SectionTitle("浏览")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("默认显示隐藏文件", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "开启后每次打开目录都会显示以 . 开头的文件",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = settings.showHiddenByDefault,
                    onCheckedChange = { vm.setShowHiddenDefault(it) }
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            Spacer(Modifier.height(16.dp))
            SectionTitle("WebDAV 服务器")

            Text("默认共享目录", style = MaterialTheme.typography.bodyLarge)
            Text("开启 WebDAV 服务器时默认使用此目录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = settings.webDavSharePath,
                onValueChange = { vm.setWebDavSharePath(it) },
                placeholder = { Text("/storage/emulated/0/Download") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("默认端口", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = settings.webDavPort.toString(),
                onValueChange = {
                    val n = it.toIntOrNull() ?: 7326
                    vm.setWebDavPort(n)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("默认用户名", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = settings.webDavUsername,
                onValueChange = { vm.setWebDavUsername(it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("默认密码", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = settings.webDavPassword,
                onValueChange = { vm.setWebDavPassword(it) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            // =========== 新增：日志区域 ===========
            Spacer(Modifier.height(16.dp))
            SectionTitle("日志")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.openLogViewer() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.BugReport, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("查看/抓取日志", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "浏览已保存的日志，或抓取当前运行日志",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()

            Spacer(Modifier.height(16.dp))
            SectionTitle("帮助")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.openHelp() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("操作说明", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "查看当前模式下的完整操作指南",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}