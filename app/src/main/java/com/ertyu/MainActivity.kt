package com.ertyu

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) {
            vm.initDefaultDirs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        requestStorageThenLoad()
    }

    private fun requestStorageThenLoad() {
        val perms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val allGranted = perms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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
    val zipPrompt by vm.zipPrompt.collectAsState()
    val zipProgress by vm.zipProgress.collectAsState()

    // ZIP 选择对话框
    zipPrompt.item?.let { item ->
        AlertDialog(
            onDismissRequest = { vm.dismissZipPrompt() },
            title = { Text(item.name, maxLines = 2) },
            text = { Text("选择解压位置") },
            confirmButton = {
                TextButton(onClick = { vm.extractZipToOtherPanel(true, item) }) {
                    Text("解压到另一侧")
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.extractZipHere(true, item) }) {
                    Text("解压到当前目录")
                }
            }
        )
    }

    // ZIP 进度对话框
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
                    if (zipProgress.total > 0) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (zipProgress.total == 0) 0f
                                else zipProgress.current.toFloat() / zipProgress.total
                            },
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

    when {
        editor.isOpen -> {
            BackHandler { vm.closeEditor() }
            EditorScreen(vm)
        }
        imageViewer.isOpen -> {
            BackHandler { vm.closeImageViewer() }
            ImageViewerScreen(vm)
        }
        else -> {
            MainScreen(vm)
        }
    }
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWide = configuration.screenWidthDp >= 600

    if (isLandscape || isWide) {
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