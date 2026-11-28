package com.ertyu.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ertyu.MainViewModel
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.ArchiveOptionsHolder
import com.ertyu.cancelCompressOptions
import com.ertyu.executeAdvancedCompress

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
    var password by remember { mutableStateOf(state.password) }
    var password2 by remember { mutableStateOf(state.password) }

    var showFormatMenu by remember { mutableStateOf(false) }
    var showLevelMenu by remember { mutableStateOf(false) }

    val formats = listOf(
        ArchiveFormat.ZIP, ArchiveFormat.TAR, ArchiveFormat.TAR_GZ,
        ArchiveFormat.TAR_BZ2, ArchiveFormat.TAR_XZ, ArchiveFormat.TAR_ZST, ArchiveFormat.SEVEN_Z
    )
    val supportsEncryption = format == ArchiveFormat.ZIP
    val passwordMismatch = supportsEncryption && password.isNotEmpty() && password != password2
    val canConfirm = fileName.isNotBlank() && !passwordMismatch

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
                                    format = f
                                    showFormatMenu = false
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
                                Text(if (level == 5) "标准压缩" else "最高压缩")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = showLevelMenu, onDismissRequest = { showLevelMenu = false }) {
                            DropdownMenuItem(text = { Text("标准压缩") }, onClick = { level = 5; showLevelMenu = false })
                            DropdownMenuItem(text = { Text("最高压缩 (更慢)") }, onClick = { level = 9; showLevelMenu = false })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ================= 加密 =================
                if (supportsEncryption) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码（留空则不加密）") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (password.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = password2,
                            onValueChange = { password2 = it },
                            label = { Text("确认密码") },
                            singleLine = true,
                            isError = passwordMismatch,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (passwordMismatch) "两次输入的密码不一致" else
                                "ZIP 将使用 AES-256 加密，解压时需要密码",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (passwordMismatch) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = if (format == ArchiveFormat.SEVEN_Z)
                            if (com.ertyu.engine.SevenZipCli.isAvailable())
                                "7z 将使用密码加密（需要 7z 客户端输入密码才能解压）"
                            else "当前安装包未内置 7zz 组件，7z 加密不可用；可改用 ZIP（AES-256）"
                        else "当前格式（${format.label}）不支持加密，密码设置已忽略",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (format == ArchiveFormat.SEVEN_Z && !com.ertyu.engine.SevenZipCli.isAvailable()) {
                        TextButton(onClick = {
                            format = ArchiveFormat.ZIP
                            fileName = if (fileName.contains('.'))
                                fileName.substringBeforeLast('.') + ".zip" else "$fileName.zip"
                        }) { Text("改用 ZIP（AES-256）") }
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
                if (deleteSource) {
                    Text(
                        text = "注意：源文件会被删除，且无法从回收站恢复（压缩完成后直接删除）。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canConfirm,
                onClick = {
                    vm.executeAdvancedCompress(
                        fileName = fileName.trim(),
                        format = format,
                        level = level,
                        separate = separate,
                        deleteSource = deleteSource,
                        toOtherPanel = toOtherPanel,
                        password = password.ifEmpty { null }
                    )
                }
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = { vm.cancelCompressOptions() }) { Text("取消") }
        }
    )
}
