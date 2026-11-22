package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.RemoteEntry
import com.ertyu.model.SftpHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SftpScreen(vm: MainViewModel) {
    val state by SftpHolder.state.collectAsState()
    val panel by vm.leftPanel.collectAsState()

    var host by remember { mutableStateOf(state.host) }
    var port by remember { mutableStateOf(state.port.toString()) }
    var user by remember { mutableStateOf(state.user) }
    var password by remember { mutableStateOf("") }
    var pendingDownload by remember { mutableStateOf<RemoteEntry?>(null) }
    var showDisconnectConfirm by remember { mutableStateOf(false) }

    val selectedUploadCount = panel.items.count {
        panel.selectedUris.contains(it.uriString) && !it.isDirectory
    }

    fun requestClose() {
        if (state.connected || state.busyText.isNotEmpty()) showDisconnectConfirm = true else vm.closeSftp()
    }

    BackHandler { requestClose() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.connected) "SFTP  ${state.user}@${state.host}" else "SFTP 连接") },
                navigationIcon = {
                    IconButton(onClick = { requestClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.connected) {
                        TextButton(onClick = { vm.sftpRefresh() }, enabled = !state.loading) { Text("刷新") }
                        TextButton(onClick = { vm.sftpUploadSelected() }, enabled = selectedUploadCount > 0) {
                            Text("上传($selectedUploadCount)")
                        }
                        TextButton(onClick = { showDisconnectConfirm = true }) { Text("断开") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (!state.connected) {
                // ================= 连接表单 =================
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("主机 / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = port,
                            onValueChange = { port = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("端口") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(110.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = user,
                            onValueChange = { user = it },
                            label = { Text("用户名") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            vm.sftpConnect(host.trim(), port.toIntOrNull() ?: 22, user.trim(), password)
                        },
                        enabled = !state.connecting && host.isNotBlank() && user.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.connecting) "连接中…" else "连接")
                    }
                    if (state.connecting) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "说明：密码只用于本次连接，不会保存；首次连接不做主机指纹校验，请仅在信任的网络使用。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // ================= 远端浏览 =================
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.path,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.path != "/") {
                        TextButton(onClick = { vm.sftpGoUp() }) { Text("上一级") }
                    }
                }
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())

                Box(Modifier.weight(1f)) {
                    if (state.entries.isEmpty() && !state.loading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("空目录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.entries, key = { it.path }) { entry ->
                                RemoteRow(
                                    entry = entry,
                                    onClick = {
                                        if (entry.isDir) vm.sftpOpen(entry.path)
                                        else pendingDownload = entry
                                    }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                if (state.busyText.isNotEmpty()) {
                    Surface(tonalElevation = 4.dp) {
                        Text(
                            text = state.busyText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        )
                    }
                }
            }

            state.error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }

    // ================= 下载确认 =================
    pendingDownload?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingDownload = null },
            title = { Text("下载文件") },
            text = {
                Column {
                    Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "大小 ${ItemSizeEngine.format(entry.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "将下载到左面板当前目录；若同名文件已存在会自动改名，不会覆盖。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDownload = null
                    vm.sftpDownload(entry)
                }) { Text("下载") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDownload = null }) { Text("取消") }
            }
        )
    }

    // ================= 断开确认 =================
    if (showDisconnectConfirm) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirm = false },
            title = { Text(if (state.busyText.isNotEmpty()) "正在传输" else "断开连接") },
            text = {
                Text(
                    if (state.busyText.isNotEmpty())
                        "当前有文件正在传输，断开会导致传输中断且不完整。确定断开吗？"
                    else "确定要断开这个 SFTP 连接吗？"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDisconnectConfirm = false
                    vm.closeSftp()
                }) { Text("断开", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirm = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun RemoteRow(entry: RemoteEntry, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (entry.isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
            contentDescription = null,
            tint = if (entry.isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = buildString {
                    if (!entry.isDir) append(ItemSizeEngine.format(entry.size)).append(" · ")
                    if (entry.modified > 0) append(dateFormat.format(Date(entry.modified)))
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
