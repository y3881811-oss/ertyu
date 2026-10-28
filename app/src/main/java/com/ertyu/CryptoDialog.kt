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
import com.ertyu.model.CryptoMode

@Composable
fun CryptoDialog(vm: MainViewModel) {
    val state by CryptoStateHolder.state.collectAsState()
    if (!state.visible) return
    
    var password by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!state.isProcessing) vm.dismissCrypto() },
        title = { Text(if (state.mode == CryptoMode.ENCRYPT) "加密文件" else "解密文件") },
        text = {
            Column {
                Text(state.fileName, maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("输入密码") },
                    singleLine = true,
                    enabled = !state.isProcessing,
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.isProcessing) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                }
                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.executeCrypto(password) },
                enabled = password.isNotEmpty() && !state.isProcessing
            ) {
                Text(if (state.mode == CryptoMode.ENCRYPT) "加密" else "解密")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.dismissCrypto() }, enabled = !state.isProcessing) {
                Text("取消")
            }
        }
    )
}