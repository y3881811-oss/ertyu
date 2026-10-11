package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.InstalledApp

private val IconBack: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
private val IconSearch: ImageVector = Icons.Default.Search
private val IconClose: ImageVector = Icons.Default.Close
private val IconRefresh: ImageVector = Icons.Default.Refresh
private val IconOpen: ImageVector = Icons.Default.OpenInNew
private val IconInfo: ImageVector = Icons.Default.Info
private val IconUninstall: ImageVector = Icons.Default.Delete
private val IconExtract: ImageVector = Icons.Default.Save
private val IconShare: ImageVector = Icons.Default.Share
private val IconFolder: ImageVector = Icons.Default.Folder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstalledAppsScreen(vm: MainViewModel) {
    val state by vm.installedAppsState.collectAsState()
    val bookmarkState by vm.bookmarkPanelState.collectAsState()
    val context = LocalContext.current

    // 记录是从哪个面板打开的（用于提取到哪个目录）
    // 复用 bookmarkPanelState.sourceIsLeft 不可靠，
    // 这里用固定策略：默认提取到左侧面板
    // 更好的做法是 MainViewModel 存一个字段，但保持简单先左面板
    val sourceIsLeft = true

    var actionTarget by remember { mutableStateOf<InstalledApp?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("已安装应用", maxLines = 1)
                        Text(
                            "共 ${state.filteredApps.size} 个",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeInstalledApps() }) {
                        Icon(IconBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshInstalledApps() }) {
                        Icon(IconRefresh, contentDescription = "刷新")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.filter,
                onValueChange = { vm.updateInstalledAppsFilter(it) },
                placeholder = { Text("搜索应用名或包名") },
                singleLine = true,
                leadingIcon = { Icon(IconSearch, null) },
                trailingIcon = {
                    if (state.filter.isNotEmpty()) {
                        IconButton(onClick = { vm.updateInstalledAppsFilter("") }) {
                            Icon(IconClose, null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { vm.toggleShowSystemApps() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "显示系统应用",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.showSystem,
                    onCheckedChange = { vm.toggleShowSystemApps() }
                )
            }

            HorizontalDivider()

            when {
                state.isLoading -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "正在加载应用列表…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                state.error != null -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "错误: ${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(
                        items = state.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        AppRow(app = app, onClick = { actionTarget = app })
                    }
                }
            }
        }
    }

    actionTarget?.let { app ->
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            title = {
                Text(app.label, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppInfoLine("包名", app.packageName)
                    AppInfoLine("版本", "${app.versionName}（${app.versionCode}）")
                    AppInfoLine("类型", if (app.isSystem) "系统应用" else "用户应用")
                    AppInfoLine("大小", app.sizeText)

                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    AppActionRow(
                        icon = IconOpen,
                        label = "打开",
                        onClick = {
                            actionTarget = null
                            vm.openApp(app, context)
                        }
                    )
                    AppActionRow(
                        icon = IconExtract,
                        label = "提取 APK 到左侧面板目录",
                        onClick = {
                            actionTarget = null
                            vm.extractAppApk(app, sourceIsLeft)
                        }
                    )
                    AppActionRow(
                        icon = IconShare,
                        label = "分享 APK",
                        onClick = {
                            actionTarget = null
                            vm.shareAppApk(app, context)
                        }
                    )
                    AppActionRow(
                        icon = IconFolder,
                        label = "打开 APK 所在目录",
                        onClick = {
                            actionTarget = null
                            vm.openAppApkFolder(app, sourceIsLeft)
                        }
                    )
                    AppActionRow(
                        icon = IconInfo,
                        label = "应用详情（系统设置）",
                        onClick = {
                            actionTarget = null
                            vm.showAppDetails(app, context)
                        }
                    )
                    AppActionRow(
                        icon = IconUninstall,
                        label = "卸载",
                        danger = true,
                        onClick = {
                            actionTarget = null
                            vm.uninstallApp(app, context)
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTarget = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun AppActionRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (danger) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

@Composable
private fun AppInfoLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(56.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRow(app: InstalledApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            val bmp = app.icon
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Text(
                    app.label.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (app.isSystem) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "系统",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}