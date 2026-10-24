package com.ertyu

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkShareScreen(vm: MainViewModel) {
    val state by NetworkShareStateHolder.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("局域网共享") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeNetworkShare() }) {
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.isRunning) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (state.isRunning) "● 运行中" else "○ 已停止",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.isRunning) {
                        Spacer(Modifier.height(8.dp))
                        Text("FTP 地址（电视/电脑端）:", style = MaterialTheme.typography.labelMedium)
                        Text("ftp://${state.ipAddress}:${state.ftpPort}", style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                        Spacer(Modifier.height(8.dp))
                        Text("网页快传（浏览器访问）:", style = MaterialTheme.typography.labelMedium)
                        Text("http://${state.ipAddress}:${state.httpPort}", style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text("共享目录", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.sharePath,
                onValueChange = { v -> vm.updateNetworkShare { it.copy(sharePath = v) } },
                singleLine = true,
                enabled = !state.isRunning,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.ftpPort.toString(),
                    onValueChange = { vm.updateNetworkShare { it.copy(ftpPort = it.toString().toIntOrNull() ?: 2121) } },
                    label = { Text("FTP 端口") },
                    singleLine = true,
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                OutlinedTextField(
                    value = state.httpPort.toString(),
                    onValueChange = { vm.updateNetworkShare { it.copy(httpPort = it.toString().toIntOrNull() ?: 7333) } },
                    label = { Text("Web 端口") },
                    singleLine = true,
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = state.ftpUsername,
                    onValueChange = { vm.updateNetworkShare { it.copy(ftpUsername = it) } },
                    label = { Text("FTP 用户名") },
                    singleLine = true,
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.padding(horizontal = 4.dp))
                OutlinedTextField(
                    value = state.ftpPassword,
                    onValueChange = { vm.updateNetworkShare { it.copy(ftpPassword = it) } },
                    label = { Text("FTP 密码") },
                    singleLine = true,
                    enabled = !state.isRunning,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.isLoading) {
                CircularProgressIndicator()
            }

            if (state.isRunning) {
                OutlinedButton(onClick = { vm.stopNetworkShare() }, modifier = Modifier.fillMaxWidth()) {
                    Text("停止共享")
                }
            } else {
                Button(onClick = { vm.startNetworkShare() }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text("启动共享")
                }
            }

            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text("错误：$it", color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "提示：\n" +
                "• FTP 适用于电视端（如 Kodi、X-plore）\n" +
                "• 网页快传请用电脑浏览器打开，可直接拖拽上传",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}