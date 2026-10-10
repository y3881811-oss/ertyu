package com.ertyu

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(vm)
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
fun MainScreen(vm: MainViewModel) {
    val left by vm.leftPanel.collectAsState()
    val right by vm.rightPanel.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        FilePanel(
            state = left,
            onOpen = { vm.openDirectory(true, it) },
            onUp = { vm.goUp(true) },
            onDelete = { vm.delete(true, it) },
            onCopy = { vm.copyToOtherPanel(true, it) },
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        FilePanel(
            state = right,
            onOpen = { vm.openDirectory(false, it) },
            onUp = { vm.goUp(false) },
            onDelete = { vm.delete(false, it) },
            onCopy = { vm.copyToOtherPanel(false, it) },
            modifier = Modifier.weight(1f)
        )
    }
}