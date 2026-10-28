package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.FileItem
import com.ertyu.model.SortMode

@Composable
fun FileActionDialog(
    item: FileItem,
    archiveFormat: ArchiveFormat?,
    isApk: Boolean,
    isImage: Boolean,
    showCrossPanel: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onExtractHere: () -> Unit,
    onExtractOther: () -> Unit,
    onZipThis: () -> Unit,
    onInfo: () -> Unit,
    onInstall: () -> Unit,
    onBrowseApk: () -> Unit,
    onHexView: () -> Unit,
    onChecksum: () -> Unit,
    onApkSign: () -> Unit,
    onApkClone: () -> Unit,
    onCrypto: () -> Unit,
    onImageTools: () -> Unit,
    onMd5Rename: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(item.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                if (isApk) {
                    ActionRow("安装", Icons.Default.InstallMobile, onInstall)
                    ActionRow("浏览内部", Icons.Default.FolderOpen, onBrowseApk)
                    ActionRow("APK 签名", Icons.Default.Edit, onApkSign)
                    ActionRow("共存/克隆", Icons.Default.ContentCopy, onApkClone)
                }
                if (isImage) {
                    ActionRow("图片处理 (压缩)", Icons.Default.Image, onImageTools)
                    ActionRow("以 MD5 重命名", Icons.Default.Fingerprint, onMd5Rename)
                }
                if (archiveFormat != null) {
                    ActionRow(
                        "解压到当前目录（${archiveFormat.label}）",
                        Icons.Default.FolderZip,
                        onExtractHere
                    )
                    if (showCrossPanel) {
                        ActionRow(
                            "解压到另一侧",
                            Icons.Default.FolderZip,
                            onExtractOther
                        )
                    }
                }
                ActionRow("压缩为…", Icons.Default.FolderZip, onZipThis)
                ActionRow("HEX 查看", Icons.Default.Search, onHexView)
                if (!item.isDirectory) {
                    ActionRow("校验和（MD5/SHA）", Icons.Default.Verified, onChecksum)
                    ActionRow(
                        if (item.name.endsWith(".enc")) "解密文件" else "加密文件（AES）",
                        Icons.Default.Lock,
                        onCrypto
                    )
                }
                ActionRow("属性", Icons.Default.Info, onInfo)
                ActionRow("重命名", Icons.Default.Edit, onRename)
                if (showCrossPanel) {
                    ActionRow(
                        "复制到另一侧",
                        Icons.Default.ContentCopy,
                        onCopy
                    )
                    ActionRow(
                        "移动到另一侧",
                        Icons.AutoMirrored.Filled.DriveFileMove,
                        onMove
                    )
                }
                ActionRow("删除", Icons.Default.Delete, onDelete)
                ActionRow("多选", Icons.Default.SelectAll, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ActionRow(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun FileInfoDialog(
    item: FileItem,
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val sizeState by vm.folderSizeState.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("文件属性") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                InfoBlock("名称", item.name)
                InfoBlock("类型", item.typeText)

                if (item.isDirectory) {
                    val countText = if (item.childCount >= 0)
                        "目录（包含 ${item.childCount} 项）" else "目录"
                    InfoBlock("内容", countText)

                    when {
                        sizeState.error != null -> {
                            InfoBlock("总大小", "计算失败：${sizeState.error}")
                        }
                        sizeState.done -> {
                            InfoBlock(
                                "总大小",
                                "${formatBytes(sizeState.totalBytes)}" +
                                        "（${sizeState.scanned} 个文件）"
                            )
                        }
                        sizeState.isCalculating -> {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "正在计算…",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "已扫描 ${sizeState.scanned} 个文件 · " +
                                        formatBytes(sizeState.totalBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            TextButton(
                                onClick = { vm.calculateFolderSize(item) }
                            ) {
                                Text("计算总大小")
                            }
                        }
                    }
                } else {
                    InfoBlock("大小", "${item.sizeText}（${item.size} 字节）")
                }

                InfoBlock("修改时间", item.lastModifiedText)
                InfoBlock("路径", item.path)

                if (sizeState.isCalculating) {
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = { vm.cancelFolderSize() }) {
                        Text("取消计算")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun InfoBlock(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}

@Composable
fun CompressFormatDialog(
    onSelect: (ArchiveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择压缩格式") },
        text = {
            Column {
                FormatRow("ZIP", ".zip（兼容性最好）", ArchiveFormat.ZIP, onSelect)
                FormatRow("TAR", ".tar（不压缩）", ArchiveFormat.TAR, onSelect)
                FormatRow("TAR.GZ", ".tar.gz（Linux 常见）", ArchiveFormat.TAR_GZ, onSelect)
                FormatRow("7Z", ".7z（高压缩比，不支持加密）", ArchiveFormat.SEVEN_Z, onSelect)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun FormatRow(
    label: String,
    desc: String,
    format: ArchiveFormat,
    onSelect: (ArchiveFormat) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(format) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FolderZip, contentDescription = null)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SortDialog(
    current: SortMode,
    onSelect: (SortMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("排序方式") },
        text = {
            Column {
                SortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == current,
                            onClick = { onSelect(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(mode.label)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
fun TextInputDialog(
    title: String,
    placeholder: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}