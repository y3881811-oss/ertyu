package com.ertyu

import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun DrawerContent(
    vm: MainViewModel,
    onDismiss: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 头部：App 图标与名称
            Column(modifier = Modifier.padding(20.dp)) {
                Icon(
                    Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.height(48.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "MTFM",
                    style = androidx.compose.material3.MaterialTheme.typography.titleLarge
                )
                Text(
                    "本地文件管理器",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            // 存储空间入口
            DrawerItem("内部存储", Icons.Default.PhoneAndroid) {
                onDismiss()
                val root = Uri.fromFile(Environment.getExternalStorageDirectory())
                vm.openDirectory(true, root)
            }
            DrawerItem("根目录", Icons.Default.Folder) {
                onDismiss()
                val root = Uri.fromFile(File("/"))
                vm.openDirectory(true, root)
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // 工具入口
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
                val settings = vm.settingsState.value
                WebDavStateHolder.state.value = WebDavStateHolder.state.value.copy(
                    sharePath = settings.webDavSharePath,
                    port = settings.webDavPort,
                    username = settings.webDavUsername,
                    password = settings.webDavPassword
                )
                vm.openWebDav()
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // 设置
            DrawerItem("设置", Icons.Default.Settings) {
                onDismiss()
                vm.openSettings()
            }

            Spacer(Modifier.height(24.dp))
        }
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