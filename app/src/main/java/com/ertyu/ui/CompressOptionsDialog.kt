package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons // 👈 补上
import androidx.compose.material.icons.filled.ArrowDropDown // 👈 补上
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.MainViewModel
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.ArchiveOptionsHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressOptionsDialog(vm: MainViewModel) {
    val state by ArchiveOptionsHolder.compressState.collectAsState()
    if (!state.visible) return

    var fileName by remember { mutableStateOf(state.fileName) }
    var format by remember { mutableStateOf(state.format) }
    var level by remember { mutableStateOf(state.compressionLevel) }
    var separate by remember { mutableStateOf(state.separateFolders) }
    var deleteSource by remember { mutableStateOf(state.deleteSource) }
    var toOtherPanel by remember { mutableStateOf(state.toOtherPanel) }

    var showFormatMenu by remember { mutableStateOf(false) }
    var showLevelMenu by remember { mutableStateOf(false) }

    val formats = listOf(
        ArchiveFormat.ZIP, ArchiveFormat.TAR, ArchiveFormat.TAR_GZ,
        ArchiveFormat.TAR_BZ2, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_ZST, ArchiveFormat.SEVEN_Z
    )

    AlertDialog(
        onDismissRequest = { vm.cancelCompressOptions() },
        title = { Text("创建压缩文件") },
        text = {
            Column {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("文件名") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // 格式下拉菜单
                    Box {
                        Column(modifier = Modifier.clickable { showFormatMenu = true }) {
                            Text("格式", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(format.label)
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = showFormatMenu, onDismissRequest = { showFormatMenu = false }) {
                            formats.forEach { f ->
                                DropdownMenuItem(text = { Text(f.label) }, onClick = { 
                                    format = f; showFormatMenu = false 
                                    // 自动更新文件名后缀
                                    fileName = fileName.substringBeforeLast(".") + "." + f.label.lowercase().replace("tar.gz", "tar.gz")
                                })
                            }
                        }
                    }

                    // 压缩级别下拉菜单
                    Box {
                        Column(modifier = Modifier.clickable { showLevelMenu = true }) {
                            Text("压缩级别", style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if(level == 5) "标准压缩" else "最高压缩")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = showLevelMenu, onDismissRequest = { showLevelMenu = false }) {
                            DropdownMenuItem(text = { Text("标准压缩") }, onClick = { level = 5; showLevelMenu = false })
                            DropdownMenuItem(text = { Text("最高压缩") }, onClick = { level = 9; showLevelMenu = false })
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { separate = !separate }) {
                    Text("单独压缩每个文件/文件夹", modifier = Modifier.weight(1f))
                    Switch(checked = separate, onCheckedChange = { separate = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { deleteSource = !deleteSource }) {
                    Text("压缩后删除源文件", modifier = Modifier.weight(1f))
                    Switch(checked = deleteSource, onCheckedChange = { deleteSource = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { toOtherPanel = !toOtherPanel }) {
                    Text("压缩到另一窗口路径", modifier = Modifier.weight(1f))
                    Switch(checked = toOtherPanel, onCheckedChange = { toOtherPanel = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.executeAdvancedCompress(fileName, format, level, separate, deleteSource, toOtherPanel)
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { vm.cancelCompressOptions() }) { Text("取消") }
        }
    )
}