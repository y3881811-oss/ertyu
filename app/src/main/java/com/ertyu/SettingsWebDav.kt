package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsWebDav(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    
    Spacer(Modifier.height(16.dp))
    SectionTitle("WebDAV 服务器")

    Text("默认共享目录", style = MaterialTheme.typography.bodyLarge)
    Text("开启 WebDAV 服务器时默认使用此目录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value = settings.webDavSharePath, onValueChange = { vm.setWebDavSharePath(it) }, placeholder = { Text("/storage/emulated/0/Download") }, singleLine = true, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(8.dp))
    Text("默认端口", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value = settings.webDavPort.toString(), onValueChange = { vm.setWebDavPort(it.toIntOrNull() ?: 7326) }, singleLine = true, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(8.dp))
    Text("默认用户名", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value = settings.webDavUsername, onValueChange = { vm.setWebDavUsername(it) }, singleLine = true, modifier = Modifier.fillMaxWidth())

    Spacer(Modifier.height(8.dp))
    Text("默认密码", style = MaterialTheme.typography.bodyLarge)
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(value = settings.webDavPassword, onValueChange = { vm.setWebDavPassword(it) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}