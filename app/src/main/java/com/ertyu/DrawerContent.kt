package com.ertyu

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ertyu.engine.StorageScanner
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DrawerContent(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    val settings = vm.settingsState.value
    val volumes = remember { StorageScanner.scanAll() }
    var showAddDialog by remember { mutableStateOf(false) }

    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text("MTFM", style = MaterialTheme.typography.titleLarge)
                Text(
                    "本地文件管理器",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()

            volumes.forEach { vol ->
                DrawerItem(vol.name, if (vol.isInternal) Icons.Default.PhoneAndroid else Icons.Default.SdCard) {
                    onDismiss()
                    vm.openDirectory(true, Uri.fromFile(File(vol.path)))
                }
            }

            if (settings.customPaths.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    "自定义路径",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                settings.customPaths.forEach { path ->
                    val name = File(path).name.ifBlank { path }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    onDismiss()
                                    vm.openDirectory(true, Uri.fromFile(File(path)))
                                },
                                onLongClick = {
                                    vm.removeCustomPath(path)
                                    vm.showToast("已删除快捷路径")
                                }
                            )
                    ) {
                        NavigationDrawerItem(
                            label = { Text(name) },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                            selected = false,
                            onClick = {},
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }

            DrawerItem("添加本地路径", Icons.Default.Add) {
                showAddDialog = true
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem("回收站", Icons.Default.DeleteForever) {
                onDismiss()
                vm.openRecycleBin()
            }
            DrawerItem("书签 / 最近访问", Icons.Default.Bookmark) {
                onDismiss()
                vm.openBookmarkPanel(true)
            }
            DrawerItem("已安装应用", Icons.Default.Apps) {
                onDismiss()
                vm.openInstalledApps()
            }
            DrawerItem("WebDAV 服务器", Icons.Default.Cloud) {
                onDismiss()
                WebDavStateHolder.state.value = WebDavStateHolder.state.value.copy(
                    sharePath = settings.webDavSharePath,
                    port = settings.webDavPort,
                    username = settings.webDavUsername,
                    password = settings.webDavPassword
                )
                vm.openWebDav()
            }
            DrawerItem("局域网共享 (FTP/Web)", Icons.Default.Share) {
                onDismiss()
                vm.openNetworkShare()
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem("设置", Icons.Default.Settings) {
                onDismiss()
                vm.openSettings()
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAddDialog) {
        var inputPath by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加本地快捷路径") },
            text = {
                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    placeholder = { Text("/storage/emulated/0/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inputPath.isNotBlank()) {
                        val f = File(inputPath)
                        if (f.exists() && f.isDirectory) {
                            vm.addCustomPath(inputPath)
                            showAddDialog = false
                        } else {
                            vm.showToast("目录不存在")
                        }
                    }
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = { Icon(icon, contentDescription = null) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}