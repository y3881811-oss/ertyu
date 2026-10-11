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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
        // 横屏或平板：双面板
        Row(modifier = Modifier.fillMaxSize()) {
            FilePanel(
                isLeft = true,
                vm = vm,
                modifier = Modifier.weight(1f)
            )
            VerticalDivider()
            FilePanel(
                isLeft = false,
                vm = vm,
                modifier = Modifier.weight(1f)
            )
        }
    } else {
        // 竖屏手机：单面板 + 可切换
        var activeLeft by remember { mutableStateOf(true) }
        FilePanel(
            isLeft = activeLeft,
            vm = vm,
            onSwitchPanel = { activeLeft = !activeLeft },
            modifier = Modifier.fillMaxSize()
        )
    }
}