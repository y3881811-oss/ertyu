package com.ertyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.engine.LanScanner
import com.ertyu.model.LanScanHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanScanScreen(vm: MainViewModel) {
    val state by LanScanHolder.state.collectAsState()
    val context = LocalContext.current
    var selected by remember { mutableStateOf<LanScanner.Host?>(null) }

    BackHandler { vm.closeLanScan() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("局域网设备") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeLanScan() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.startLanScan() }, enabled = !state.scanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "重新扫描")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Text(
                text = if (state.selfIp.isBlank()) "未连接局域网"
                else "本机 ${state.selfIp} · 已扫描 ${state.done}/${state.total}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
            if (state.scanning) LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))

            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(Modifier.weight(1f)) {
                if (state.hosts.isEmpty() && !state.scanning) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "没发现设备。\n可能路由器开了「AP 隔离」，或者对方没开 SMB/SFTP/FTP。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.hosts, key = { it.ip }) { host ->
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { selected = host }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(host.ip, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                                    Text(
                                        text = buildString {
                                            if (host.hasSmb) append("SMB ")
                                            if (host.hasSftp) append("SFTP ")
                                            if (host.hasFtp) append("FTP ")
                                            if (isEmpty()) append("已开放端口 ${host.ports.joinToString()}")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "连接",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    selected?.let { host ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(host.ip) },
            text = {
                Column {
                    Text("开放服务：" + host.ports.joinToString("、"))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "SMB 适合电脑共享文件夹；SFTP 适合装了 SSH 的服务器/NAS。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selected = null
                    vm.connectSftpTo(host.ip)
                }, enabled = host.hasSftp) { Text("用 SFTP 连接") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        copyToClipboard(context, host.ip)
                        selected = null
                        vm.showToast("已复制 ${host.ip}")
                    }) { Text("复制 IP") }
                    TextButton(onClick = {
                        selected = null
                        vm.openSmbDialog()
                    }) { Text(if (host.hasSmb) "打开 SMB" else "SMB") }
                }
            }
        )
    }
}

private fun copyToClipboard(context: Context, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("ip", text))
    } catch (_: Exception) {
    }
}
