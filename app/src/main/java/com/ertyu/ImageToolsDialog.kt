package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
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

@Composable
fun ImageToolsDialog(vm: MainViewModel) {
    val targetItem by vm.imageToolTarget.collectAsState()
    val isProcessing by vm.imageToolProcessing.collectAsState()

    if (targetItem != null) {
        val item = targetItem!!
        var quality by remember { mutableStateOf("80") }

        AlertDialog(
            onDismissRequest = { if (!isProcessing) vm.dismissImageTools() },
            title = { Text("图片处理 - ${item.name}") },
            text = {
                Column {
                    Text("压缩图片 (JPEG)", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quality,
                        onValueChange = { quality = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("压缩质量 (1-100)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.compressImage(item, quality.toIntOrNull() ?: 80) },
                    enabled = !isProcessing
                ) { Text(if (isProcessing) "处理中..." else "开始压缩") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissImageTools() }, enabled = !isProcessing) {
                    Text("关闭")
                }
            }
        )
    }
}