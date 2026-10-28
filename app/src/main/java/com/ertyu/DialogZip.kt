package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
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
import com.ertyu.model.ArchiveFormat

@Composable
fun DialogZip(vm: MainViewModel) {
    val zipPrompt by vm.zipPrompt.collectAsState()
    zipPrompt.item?.let { item ->
        val format = ArchiveFormat.detect(item.name)
        AlertDialog(
            onDismissRequest = { vm.dismissZipPrompt() },
            title = { Text(item.name, maxLines = 2) },
            text = { Text(if (format != null) "格式：${format.label}\n选择解压位置" else "选择解压位置") },
            confirmButton = { TextButton(onClick = { vm.extractZipToOtherPanel(true, item) }) { Text("解压到另一侧") } },
            dismissButton = { TextButton(onClick = { vm.extractZipHere(true, item) }) { Text("解压到当前目录") } }
        )
    }

    val zipProgress by vm.zipProgress.collectAsState()
    if (zipProgress.visible) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (zipProgress.done || zipProgress.error != null || zipProgress.needsPassword) vm.dismissZipProgress() },
            title = { Text(if (zipProgress.needsPassword) "需要密码" else zipProgress.title) },
            text = {
                Column {
                    if (zipProgress.needsPassword) {
                        Text("此压缩包已加密，请输入密码：", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = passwordInput, onValueChange = { passwordInput = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    } else {
                        Text(zipProgress.message, maxLines = 2)
                        Spacer(Modifier.height(8.dp))
                        if (zipProgress.total > 0) {
                            val progressValue = (zipProgress.current.toFloat() / zipProgress.total.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(progress = { if (progressValue.isNaN()) 0f else progressValue }, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text("${zipProgress.current} / ${zipProgress.total}")
                        } else if (zipProgress.current > 0) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(4.dp))
                            Text("已处理 ${zipProgress.current} 项")
                        }
                        zipProgress.error?.let { Spacer(Modifier.height(8.dp)); Text("错误: $it", color = MaterialTheme.colorScheme.error) }
                    }
                }
            },
            confirmButton = {
                if (zipProgress.needsPassword) TextButton(onClick = { vm.submitZipPassword(passwordInput) }, enabled = passwordInput.isNotEmpty()) { Text("解密") }
                else if (zipProgress.done || zipProgress.error != null) TextButton(onClick = { vm.dismissZipProgress() }) { Text(if (zipProgress.error != null) "关闭" else "完成") }
            },
            dismissButton = { if (zipProgress.needsPassword) TextButton(onClick = { vm.dismissZipProgress() }) { Text("取消") } }
        )
    }
}