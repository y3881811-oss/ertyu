package com.ertyu

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ertyu.engine.ApkInstaller
import com.ertyu.engine.PackageChangeMonitor
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.PanelMode
import java.io.File

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            vm.initDefaultDirs()
        }
    }

    private val installReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != ApkInstaller.ACTION_INSTALL_RESULT) return
            val status = intent.getIntExtra(
                android.content.pm.PackageInstaller.EXTRA_STATUS,
                -1
            )
            if (status ==
                android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION
            ) {
                @Suppress("DEPRECATION")
                val confirmIntent = intent.getParcelableExtra<Intent>(
                    android.content.Intent.EXTRA_INTENT
                )
                if (confirmIntent != null) {
                    runCatching { startActivity(confirmIntent) }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(ApkInstaller.ACTION_INSTALL_RESULT)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                installReceiver, filter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(installReceiver, filter)
        }

        PackageChangeMonitor.start(this)

        setContent {
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme() else lightColorScheme()
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(vm)
                }
            }
        }
        vm.loadSettings()
        requestStorageThenLoad()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(installReceiver) }
        PackageChangeMonitor.stop(this)
        super.onDestroy()
    }

    private fun requestStorageThenLoad() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            vm.initDefaultDirs()
        } else {
            permissionLauncher.launch(perms)
        }
    }
}

@Composable
fun AppRoot(vm: MainViewModel) {
    val editor by vm.editor.collectAsState()
    val imageViewer by vm.imageViewer.collectAsState()
    val hexViewerState by vm.hexViewerState.collectAsState()
    val zipPrompt by vm.zipPrompt.collectAsState()
    val zipProgress by vm.zipProgress.collectAsState()
    val searchState by vm.searchState.collectAsState()
    val apkState by vm.apkInfoState.collectAsState()
    val apkBrowserState by vm.apkBrowserState.collectAsState()
    val pendingInstall by vm.pendingInstall.collectAsState()
    val installState by vm.installState.collectAsState()
    val bookmarkState by vm.bookmarkPanelState.collectAsState()
    val newFilePrompt by vm.newFilePrompt.collectAsState()
    val installedAppsState by vm.installedAppsState.collectAsState()
    val settingsState by vm.settingsState.collectAsState()
    val settingsVisible by vm.settingsVisible.collectAsState()
    val helpVisible by vm.helpVisible.collectAsState()

    val newFolderVisible by vm.newFolderDialogVisible.collectAsState()
    val newFolderIsLeft by vm.newFolderDialogIsLeft.collectAsState()
    val jumpVisible by vm.jumpDialogVisible.collectAsState()
    val jumpIsLeft by vm.jumpDialogIsLeft.collectAsState()
    val sortVisible by vm.sortDialogVisible.collectAsState()
    val sortIsLeft by vm.sortDialogIsLeft.collectAsState()
    val actionTarget by vm.actionDialogTarget.collectAsState()
    val actionIsLeft by vm.actionDialogIsLeft.collectAsState()
    val infoTarget by vm.infoDialogTarget.collectAsState()
    val renameTarget by vm.renameDialogTarget.collectAsState()
    val renameIsLeft by vm.renameDialogIsLeft.collectAsState()
    val compressVisible by vm.compressPromptVisible.collectAsState()

    val context = LocalContext.current

    // 批量重命名 / 校验和 对话框
    BatchRenameDialog(vm)
    ChecksumDialog(vm)

    // ZIP 解压位置
    zipPrompt.item?.let { item ->
        val format = ArchiveFormat.detect(item.name)
        AlertDialog(
            onDismissRequest = { vm.dismissZipPrompt() },
            title = { Text(item.name, maxLines = 2) },
            text = {
                Text(
                    if (format != null)
                        "格式：${format.label}\n选择解压位置"
                    else "选择解压位置"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.extractZipToOtherPanel(true, item)
                }) { Text("解压到另一侧") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.extractZipHere(true, item)
                }) { Text("解压到当前目录") }
            }
        )
    }

    // 压缩/解压进度
    if (zipProgress.visible) {
        AlertDialog(
            onDismissRequest = {
                if (zipProgress.done || zipProgress.error != null) {
                    vm.dismissZipProgress()
                }
            },
            title = { Text(zipProgress.title) },
            text = {
                Column {
                    Text(zipProgress.message, maxLines = 2)
                    Spacer(Modifier.height(8.dp))
                    if (zipProgress.total > 0) {
                        LinearProgressIndicator(
                            progress = {
                                zipProgress.current.toFloat() / zipProgress.total
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${zipProgress.current} / ${zipProgress.total}")
                    } else if (zipProgress.current > 0) {
                        Text("已处理 ${zipProgress.current} 项")
                    }
                    zipProgress.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "错误: $it",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (zipProgress.done || zipProgress.error != null) {
                    TextButton(onClick = { vm.dismissZipProgress() }) {
                        Text(if (zipProgress.error != null) "关闭" else "完成")
                    }
                }
            }
        )
    }

    // 安装进度
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
                            progress = {
                                installState.copied.toFloat() / installState.total
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${installState.percent}%")
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        installState.stage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    installState.error?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "错误: $it",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                if (installState.error != null) {
                    TextButton(onClick = { vm.dismissInstallProgress() }) {
                        Text("关闭")
                    }
                }
            }
        )
    }

    // 安装权限引导
    if (pendingInstall != null) {
        AlertDialog(
            onDismissRequest = { vm.dismissInstallPermissionPrompt() },
            title = { Text("需要安装权限") },
            text = {
                Text(
                    "本应用需要「安装未知应用」权限才能安装 APK。\n\n" +
                            "点击「去授权」后，在系统设置里开启本应用的开关，" +
                            "返回后再次点击安装即可。"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    ApkInstaller.openInstallPermissionSettings(context)
                    vm.dismissInstallPermissionPrompt()
                }) { Text("去授权") }
            },
            dismissButton = {
                TextButton(onClick = { vm.dismissInstallPermissionPrompt() }) {
                    Text("取消")
                }
            }
        )
    }

    // 新建文件
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
                TextButton(onClick = { vm.dismissNewFilePrompt() }) {
                    Text("取消")
                }
            }
        )
    }

    // 新建文件夹
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
                        vm.openDirectory(
                            jumpIsLeft,
                            android.net.Uri.fromFile(dir)
                        )
                    }
                }
                vm.dismissJumpDialog()
            },
            onDismiss = { vm.dismissJumpDialog() }
        )
    }

    // 排序方式
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
    actionTarget?.let { target ->
        val format = ArchiveFormat.detect(target.name)
        val isApk = target.name.substringAfterLast('.', "")
            .lowercase() == "apk"
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
            }
        )
    }

    // 属性对话框
    infoTarget?.let { target ->
        FileInfoDialog(
            item = target,
            vm = vm,
            onDismiss = { vm.dismissInfoDialog() }
        )
    }

    // 重命名
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
    if (compressVisible) {
        CompressFormatDialog(
            onSelect = { format -> vm.compressWithFormat(format) },
            onDismiss = { vm.dismissCompressPrompt() }
        )
    }

    // 页面路由
    when {
        editor.isOpen -> {
            BackHandler { vm.closeEditor() }
            EditorScreen(vm)
        }
        imageViewer.isOpen -> {
            BackHandler { vm.closeImageViewer() }
            ImageViewerScreen(vm)
        }
        hexViewerState.visible -> {
            BackHandler { vm.closeHexViewer() }
            HexViewerScreen(vm)
        }
        searchState.visible -> {
            BackHandler { vm.closeSearch() }
            SearchScreen(vm)
        }
        apkState.visible -> {
            BackHandler { vm.closeApkInfo() }
            ApkInfoScreen(vm)
        }
        apkBrowserState.visible -> {
            BackHandler { vm.closeApkBrowser() }
            ApkBrowserScreen(vm)
        }
        bookmarkState.visible -> {
            BackHandler { vm.closeBookmarkPanel() }
            BookmarkScreen(vm)
        }
        installedAppsState.visible -> {
            BackHandler { vm.closeInstalledApps() }
            InstalledAppsScreen(vm)
        }
        settingsVisible -> {
            BackHandler { vm.closeSettings() }
            SettingsScreen(vm)
        }
        helpVisible -> {
            BackHandler { vm.closeHelp() }
            HelpScreen(vm)
        }
        else -> {
            MainScreen(vm)
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val settings by vm.settingsState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWide = configuration.screenWidthDp >= 600

    val showDual = when (settings.panelMode) {
        PanelMode.DUAL -> true
        PanelMode.SINGLE -> false
        PanelMode.AUTO -> isLandscape || isWide
    }

    if (showDual) {
        Row(modifier = Modifier.fillMaxSize()) {
            FilePanel(isLeft = true, vm = vm, modifier = Modifier.weight(1f))
            VerticalDivider()
            FilePanel(isLeft = false, vm = vm, modifier = Modifier.weight(1f))
        }
    } else {
        var activeLeft by remember { mutableStateOf(true) }
        FilePanel(
            isLeft = activeLeft,
            vm = vm,
            onSwitchPanel = { activeLeft = !activeLeft },
            modifier = Modifier.fillMaxSize()
        )
    }
}