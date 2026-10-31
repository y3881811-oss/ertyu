package com.ertyu

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
            val dark = isSystemInDarkTheme()
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
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