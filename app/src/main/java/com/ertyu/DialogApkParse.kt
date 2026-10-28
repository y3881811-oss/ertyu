package com.ertyu

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun DialogApkParse(vm: MainViewModel) {
    val state by vm.pendingParseState.collectAsState()
    
    if (state.visible) {
        AlertDialog(
            onDismissRequest = { vm.cancelApkInternalParse() },
            title = { Text("解析确认") },
            text = {
                Text("是否加载并解析「${state.entryName}」？\n\n如果是大型资源文件，解析可能需要几秒钟。")
            },
            confirmButton = {
                TextButton(onClick = { vm.confirmApkInternalParse() }) {
                    Text("确认解析")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelApkInternalParse() }) {
                    Text("取消")
                }
            }
        )
    }
}