package com.ertyu

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.ertyu.engine.ApkInstaller
import com.ertyu.engine.PackageChangeMonitor
import com.ertyu.engine.PermissionHelper

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
                android.content.pm.PackageInstaller.EXTRA_STATUS, -1
            )
            if (status == android.content.pm.PackageInstaller.STATUS_PENDING_USER_ACTION) {
                @Suppress("DEPRECATION")
                val confirmIntent = intent.getParcelableExtra<Intent>(android.content.Intent.EXTRA_INTENT)
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
            registerReceiver(installReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(installReceiver, filter)
        }

        PackageChangeMonitor.start(this)

        setContent {
            val settings by vm.settingsState.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.themeMode) {
                1 -> false
                2 -> true
                else -> systemDark
            }
            val accent = com.ertyu.ui.AccentPalette.colorOf(settings.accentColor)
            // 硬件键盘快捷键（平板/外接键盘）：F5 刷新、Ctrl+H 隐藏文件、Ctrl+G 网格
            val keyFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { keyFocus.requestFocus() } }
            fun handleShortcut(event: androidx.compose.ui.input.key.KeyEvent): Boolean {
                if (event.type != KeyEventType.KeyDown) return false
                if (event.isCtrlPressed) {
                    return when (event.key) {
                        Key.H -> { vm.setShowHiddenDefault(!settings.showHiddenByDefault); true }
                        Key.G -> { vm.toggleGridMode(); true }
                        else -> false
                    }
                }
                return when (event.key) {
                    Key.F5 -> { vm.refresh(true); vm.refresh(false); true }
                    else -> false
                }
            }

            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(primary = accent)
                else lightColorScheme(primary = accent)
            ) {
                CompositionLocalProvider(com.ertyu.ui.LocalListDensity provides settings.listDensity) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .focusRequester(keyFocus)
                                .focusable()
                                .onPreviewKeyEvent { handleShortcut(it) }
                        ) {
                            AppRoot(vm)
                        }
                    }
                }
            }
        }
        vm.loadSettings()
        requestStorageThenLoad()
        collectSharedFiles(intent)

        // ===== 通知权限请求：移到 onCreate 内部 =====
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        collectSharedFiles(intent)
    }

    /** 处理「分享到 EYU文件管理器」/ 打开方式传入的文件 */
    private fun collectSharedFiles(intent: Intent?) {
        if (intent == null) return
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val uris = ArrayList<android.net.Uri>()
        @Suppress("DEPRECATION")
        val single = intent.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        if (single != null) uris.add(single)
        @Suppress("DEPRECATION")
        val multiple = intent.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
        multiple?.forEach { if (!uris.contains(it)) uris.add(it) }
        if (uris.isEmpty()) return

        com.ertyu.model.IncomingShareHolder.uris.value = uris
        vm.showToast("收到 ${uris.size} 个文件，正在导入…")
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
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            vm.initDefaultDirs()
            checkAndroid11Permission()
        } else {
            permissionLauncher.launch(perms)
        }
    }

    private fun checkAndroid11Permission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!PermissionHelper.hasManageExternalStoragePermission()) {
                PermissionHelper.requestManageExternalStoragePermission(this)
            }
        }
    }
}