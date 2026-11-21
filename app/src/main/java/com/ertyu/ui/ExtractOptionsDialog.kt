package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.MainViewModel
import com.ertyu.model.ArchiveOptionsHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractOptionsDialog(vm: MainViewModel) {
    val state by ArchiveOptionsHolder.extractState.collectAsState()
    if (!state.visible) return

    var folderName by remember { mutableStateOf(state.folderName) }
    var toOtherPanel by remember { mutableStateOf(state.toOtherPanel) }
    var deleteSource by remember { mutableStateOf(state.deleteSource) }
    var isFolderMode by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = { vm.cancelExtractOptions() },
        title = { Text("解压") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isFolderMode = false }) {
                    RadioButton(selected = !isFolderMode, onClick = { isFolderMode = false })
                    Text("解压到当前目录")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { isFolderMode = true }) {
                    RadioButton(selected = isFolderMode, onClick = { isFolderMode = true })
                    Text("解压到文件夹...")
                }
                
                if (isFolderMode) {
                    OutlinedTextField(
                        value = folderName,
                        onValueChange = { folderName = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { toOtherPanel = !toOtherPanel }) {
                    Checkbox(checked = toOtherPanel, onCheckedChange = { toOtherPanel = it })
                    Text("解压到另一窗口路径")
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { deleteSource = !deleteSource }) {
                    Checkbox(checked = deleteSource, onCheckedChange = { deleteSource = it })
                    Text("解压后删除源文件")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.executeAdvancedExtract(if (isFolderMode) folderName else "", toOtherPanel, deleteSource)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { vm.cancelExtractOptions() }) { Text("关闭") }
        }
    )
}