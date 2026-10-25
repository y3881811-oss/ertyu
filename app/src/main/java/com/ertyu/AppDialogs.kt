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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ApkInstaller
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.PanelMode
import java.io.File

@Composable
fun AppDialogs(vm: MainViewModel) {
    val context = LocalContext.current

    BatchRenameDialog(vm)
    ChecksumDialog(vm)
    ApkCloneDialog(vm)

    // ZIP 解压位置选择
    val zipPrompt by vm.zipPrompt.collectAsState()
    zipPrompt.item?.let { item ->
        val format = ArchiveFormat.detect(item.name)
        AlertDialog(
            onDismissRequest = { vm.dismissZipPrompt() },
            title = { Text(item.name, maxLines = 2) },
            text = { Text(if (format != null) "格式：${format.label}\n选择解压位置" else "选择解压位置") },
            confirmButton = {
                TextButton(onClick = { vm.extractZipToOtherPanel(true, item) }) { Text("解压到另一侧") }
            },
            dismissButton = {
                TextButton(onClick = { vm.extractZipHere(true, item) }) { Text("解压到当前目录") }
            }
        )
    }

    // ZIP 进度与密码输入
    val zipProgress by vm.zipProgress.collectAsState()
    if (zipProgress.visible) {
        var passwordInput by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = {
                if (zipProgress.done || zipProgress.error != null || zipProgress.needsPassword) {
                    vm.dismissZipProgress()
                }
            },
            title = { Text(if (zipProgress.needsPassword) "需要密码" else zipProgress.title) },
            text = {
                Column {
                    if (zipProgress.needsPassword) {
                        Text("此压缩包已加密，请输入密码：", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(zipProgress.message, maxLines = 2)
                        Spacer(Modifier.height(8.dp))
                        if (zipProgress.total > 0) {
                            LinearProgressIndicator(
                                progress = { zipProgress.current.toFloat() / zipProgress.total },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text("${zipProgress.current} / ${zipProgress.total}")
                        }
                        zipProgress.error?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("错误: $it", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                if (zipProgress.needsPassword) {
                    TextButton(
                        onClick = { vm.submitZipPassword(passwordInput) },
                        enabled = passwordInput.isNotEmpty()
                    ) { Text("解密") }
                } else if (zipProgress.done || zipProgress.error != null) {
                    TextButton(onClick = { vm.dismissZipProgress() }) {
                        Text(if (zipProgress.error != null) "关闭" else "完成")
                    }
                }
            },
            dismissButton = {
                if (zipProgress.needsPassword) {
                    TextButton(onClick = { vm.dismissZipProgress() }) { Text("取消") }
                }
            }
        )
    }

    // 安装进度
    val installState by vm.installState.collectAsState()
    if (installState.visible) {
        AlertDialog(
            onDismissRequest = {
                if (installState.error != null) vm.dismissInstallProgress()
            },
            title = { Text("正在安装") },
            text = {
                Column {
                    Text(installState.fileName, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    if (installState.total > 0) {
                        LinearProgressIndicator(
                            progress = { installState.copied.toFloat() / installState.total },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${installState.percent}%")
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        installState.stage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    installState.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("错误: $it", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                if (installState.error != null) {
                    TextButton(onClick = { vm.dismissInstallProgress() }) { Text("关闭") }
                }
            }
        )
    }

    // 安装权限引导
    val pendingInstall by vm.pendingInstall.collectAsState()
    if (pendingInstall != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissInstallPermissionPrompt() },
            title = { Text("需要安装权限") },
            text = {
                Text("本应用需要「安装未知应用」权限才能安装 APK。\n\n点击「去授权」后，在系统设置里开启本应用的开关，返回后再次点击安装即可。")
            },
            confirmButton = {
                TextButton(onClick = {
                    ApkInstaller.openInstallPermissionSettings(context)
                    vm.dismissInstallPermissionPrompt()
                }) { Text("去授权") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissInstallPermissionPrompt() }) { Text("取消") }
            }
        )
    }

    // 新建文件
    val newFilePrompt by vm.newFilePrompt.collectAsState()
    if (newFilePrompt) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { vm.dismissNewFilePrompt() },
            title = { Text("新建文件") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("文件名（含扩展名）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) vm.createFile(text)
                    else vm.dismissNewFilePrompt()
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissNewFilePrompt() }) { Text("取消") }
            }
        )
    }

    // 新建文件夹
    val newFolderVisible by vm.newFolderDialogVisible.collectAsState()
    val newFolderIsLeft by vm.newFolderDialogIsLeft.collectAsState()
    if (newFolderVisible) {
        TextInputDialog(
            title = "新建文件夹",
            placeholder = "文件夹名称",
            initial = "",
            onConfirm = { name ->
                if (name.isNotBlank()) vm.createDirectory(newFolderIsLeft, name)
                vm.dismissNewFolderDialog()
            },
            onDismiss = { vm.dismissNewFolderDialog() }
        )
    }

    // 跳转路径
    val jumpVisible by vm.jumpDialogVisible.collectAsState()
    val jumpIsLeft by vm.jumpDialogIsLeft.collectAsState()
    if (jumpVisible) {
        val currentPath = vm.flowOf(jumpIsLeft).value.currentUri?.path ?: ""
        TextInputDialog(
            title = "跳转到路径",
            placeholder = "/storage/emulated/0/...",
            initial = currentPath,
            onConfirm = { input ->
                if (input.isNotBlank()) {
                    val dir = File(input)
                    if (dir.exists() && dir.isDirectory) {
                        vm.openDirectory(jumpIsLeft, android.net.Uri.fromFile(dir))
                    }
                }
                vm.dismissJumpDialog()
            },
            onDismiss = { vm.dismissJumpDialog() }
        )
    }

    // 排序方式
    val sortVisible by vm.sortDialogVisible.collectAsState()
    val sortIsLeft by vm.sortDialogIsLeft.collectAsState()
    if (sortVisible) {
        val currentSort = vm.flowOf(sortIsLeft).value.sortMode
        SortDialog(
            current = currentSort,
            onSelect = {
                vm.setSortMode(sortIsLeft, it)
                vm.dismissSortDialog()
            },
            onDismiss = { vm.dismissSortDialog() }
        )
    }

    // 文件操作菜单
    val actionTarget by vm.actionDialogTarget.collectAsState()
    val actionIsLeft by vm.actionDialogIsLeft.collectAsState()
    val settingsState by vm.settingsState.collectAsState()
    
    actionTarget?.let { target ->
        val format = ArchiveFormat.detect(target.name)
        val isApk = target.name.substringAfterLast('.', "").lowercase() == "apk"
        val showCrossPanel = settingsState.panelMode != PanelMode.SINGLE
        FileActionDialog(
            item = target,
            archiveFormat = format,
            isApk = isApk,
            showCrossPanel = showCrossPanel,
            onDismiss = { vm.dismissActionDialog() },
            onRename = {
                vm.dismissActionDialog()
                vm.openRenameDialog(actionIsLeft, target)
            },
            onCopy = {
                vm.dismissActionDialog()
                vm.copyToOtherPanel(actionIsLeft, target)
            },
            onMove = {
                vm.dismissActionDialog()
                vm.moveToOtherPanel(actionIsLeft, target)
            },
            onDelete = {
                vm.dismissActionDialog()
                vm.delete(actionIsLeft, target)
            },
            onSelect = {
                vm.dismissActionDialog()
                vm.enterSelection(actionIsLeft, target.uriString)
            },
            onExtractHere = {
                vm.dismissActionDialog()
                vm.extractZipHere(actionIsLeft, target)
            },
            onExtractOther = {
                vm.dismissActionDialog()
                vm.extractZipToOtherPanel(actionIsLeft, target)
            },
            onZipThis = {
                vm.dismissActionDialog()
                vm.enterSelection(actionIsLeft, target.uriString)
                vm.requestCompress(actionIsLeft)
            },
            onInfo = {
                vm.dismissActionDialog()
                vm.openInfoDialog(target)
            },
            onInstall = {
                vm.dismissActionDialog()
                vm.installApk(target)
            },
            onBrowseApk = {
                vm.dismissActionDialog()
                vm.openApkBrowser(actionIsLeft, target)
            },
            onHexView = {
                vm.dismissActionDialog()
                vm.openHexViewer(target)
            },
            onChecksum = {
                vm.dismissActionDialog()
                vm.openChecksum(target)
            },
            onApkSign = {
                vm.dismissActionDialog()
                vm.openApkSigner(target)
            },
            onApkClone = {
                vm.dismissActionDialog()
                vm.openApkClone(target)
            },
            onCrypto = {
                vm.dismissActionDialog()
                vm.openCrypto(actionIsLeft, target)
            }
        )
    }

    // 属性对话框
    val infoTarget by vm.infoDialogTarget.collectAsState()
    infoTarget?.let { target ->
        FileInfoDialog(
            item = target,
            vm = vm,
            onDismiss = { vm.dismissInfoDialog() }
        )
    }

    // 重命名
    val renameTarget by vm.renameDialogTarget.collectAsState()
    val renameIsLeft by vm.renameDialogIsLeft.collectAsState()
    renameTarget?.let { target ->
        TextInputDialog(
            title = "重命名",
            placeholder = "新名称",
            initial = target.name,
            onConfirm = { newName ->
                if (newName.isNotBlank() && newName != target.name) {
                    vm.rename(renameIsLeft, target, newName)
                }
                vm.dismissRenameDialog()
            },
            onDismiss = { vm.dismissRenameDialog() }
        )
    }

    // 压缩格式选择
    val compressVisible by vm.compressPromptVisible.collectAsState()
    if (compressVisible) {
        CompressFormatDialog(
            onSelect = { format -> vm.compressWithFormat(format) },
            onDismiss = { vm.dismissCompressPrompt() }
        )
    }
}