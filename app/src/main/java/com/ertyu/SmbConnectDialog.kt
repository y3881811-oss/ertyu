package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.SmbConfig

@Composable
fun SmbConnectDialog(vm: MainViewModel) {
    val state by SmbStateHolder.state.collectAsState()
    
    if (state.visible) {
        var host by remember { mutableStateOf("") }
        var share by remember { mutableStateOf("") }
        var user by remember { mutableStateOf("") }
        var pass by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { if (!state.isLoading) vm.closeSmbDialog() },
            title = { Text("连接 SMB 共享") },
            text = {
                Column {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("IP 地址 (如 192.168.1.100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = share,
                        onValueChange = { share = it },
                        label = { Text("共享文件夹名 (如 D$)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = user,
                        onValueChange = { user = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pass,
                        onValueChange = { pass = it },
                        label = { Text("密码") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (state.isLoading) {
                        Spacer(Modifier.height(12.dp))
                        CircularProgressIndicator()
                    }
                    
                    state.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.connectSmb(SmbConfig(host, share, user, pass)) },
                    enabled = !state.isLoading && host.isNotBlank() && share.isNotBlank()
                ) { Text("连接") }
            },
            dismissButton = {
                TextButton(onClick = { vm.closeSmbDialog() }, enabled = !state.isLoading) {
                    Text("取消")
                }
            }
        )
    }
}