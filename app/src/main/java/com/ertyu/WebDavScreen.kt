package com.ertyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavScreen(vm: MainViewModel) {
    val state by WebDavStateHolder.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WebDAV 服务器") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isRunning) vm.stopWebDav()
                        vm.closeWebDav()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isRunning)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (state.isRunning) "● 运行中" else "○ 已停止",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isRunning && state.serverUrl.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                state.serverUrl,
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val cm = context.getSystemService(
                                    Context.CLIPBOARD_SERVICE
                                ) as ClipboardManager
                                cm.setPrimaryClip(
                                    ClipData.newPlainText("url", state.serverUrl)
                                )
                                vm.showToast("已复制地址")
                            }) {
                                Icon(
                                    Icons.Default.ContentCopy,
                                    contentDescription = "复制"
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "在电视/电脑的文件管理器里添加 WebDAV 服务器，" +
                                    "地址如上，用户名 ${state.username}，" +
                                    "密码 ${state.password}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("共享目录", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.sharePath,
                onValueChange = { v ->
                    vm.updateWebDav { it.copy(sharePath = v) }
                },
                placeholder = { Text("/storage/emulated/0/Download") },
                singleLine = true,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("端口", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.port.toString(),
                onValueChange = { v ->
                    val n = v.toIntOrNull() ?: 7326
                    vm.updateWebDav { it.copy(port = n) }
                },
                singleLine = true,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("用户名", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.username,
                onValueChange = { v ->
                    vm.updateWebDav { it.copy(username = v) }
                },
                singleLine = true,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Text("密码", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.password,
                onValueChange = { v ->
                    vm.updateWebDav { it.copy(password = v) }
                },
                singleLine = true,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(8.dp))
            }

            if (state.isRunning) {
                OutlinedButton(
                    onClick = { vm.stopWebDav() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("停止服务器")
                }
            } else {
                Button(
                    onClick = { vm.startWebDav() },
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("启动服务器")
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    "错误：$it",
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "提示：\n" +
                        "• 手机和电视需在同一 WiFi 下\n" +
                        "• 电视端添加「WebDAV」协议\n" +
                        "• 支持上传/下载/新建文件夹/删除/重命名",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}