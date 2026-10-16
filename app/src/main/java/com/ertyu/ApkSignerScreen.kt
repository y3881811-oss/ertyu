package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkSignerScreen(vm: MainViewModel) {
    val state by ApkToolsState.signerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("APK 签名") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeApkSigner() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("待签名文件：", style = MaterialTheme.typography.labelMedium)
            Text(
                state.apkName,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(16.dp))

            Text("Keystore：", style = MaterialTheme.typography.labelMedium)
            OutlinedTextField(
                value = state.keystorePath,
                onValueChange = { v ->
                    vm.updateApkSigner {
                        it.copy(
                            keystorePath = v,
                            keystoreName = v.substringAfterLast('/')
                        )
                    }
                },
                placeholder = { Text("/sdcard/xxx.jks") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.storePass,
                onValueChange = { v ->
                    vm.updateApkSigner { it.copy(storePass = v) }
                },
                label = { Text("Store 密码") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.keyAlias,
                onValueChange = { v ->
                    vm.updateApkSigner { it.copy(keyAlias = v) }
                },
                label = { Text("Key 别名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.keyPass,
                onValueChange = { v ->
                    vm.updateApkSigner { it.copy(keyPass = v) }
                },
                label = { Text("Key 密码（留空则同 Store 密码）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { vm.executeApkSign() },
                enabled = !state.isSigning,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.isSigning) "签名中…" else "开始签名")
            }

            if (state.isSigning || state.progressMessage.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                if (state.isSigning) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    state.progressMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            state.error?.let {
                Spacer(Modifier.height(16.dp))
                Text(
                    "错误：$it",
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (state.resultPath.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("输出文件：", style = MaterialTheme.typography.labelMedium)
                Text(
                    state.resultPath,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}