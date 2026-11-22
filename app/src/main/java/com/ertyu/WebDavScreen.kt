package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.WebDavHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebDavScreen(vm: MainViewModel) {
    val state by WebDavHolder.state.collectAsState()
    var passVisible by remember { mutableStateOf(false) }
    val uploadLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.webDavUpload(uris) }

    BackHandler { vm.closeWebDav() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.connected) "WebDAV ${state.path}" else "连接 WebDAV") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.connected) {
                            com.ertyu.model.WebDavHolder.state.value =
                                state.copy(connected = false, entries = emptyList())
                        } else vm.closeWebDav()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.connected) {
                        IconButton(onClick = { vm.webDavUploadOpen(uploadLauncher) }) {
                            Icon(Icons.Default.Upload, contentDescription = "上传")
                        }
                        IconButton(onClick = { vm.webDavRefresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!state.connected) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.url,
                        onValueChange = { vm.setWebDavUrl(it) },
                        label = { Text("服务器地址") },
                        placeholder = { Text("http://192.168.1.10:5005/dav") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.user,
                        onValueChange = { vm.setWebDavUser(it) },
                        label = { Text("用户名（可选）") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.pass,
                        onValueChange = { vm.setWebDavPass(it) },
                        label = { Text("密码（可选）") },
                        singleLine = true,
                        visualTransformation = if (passVisible) androidx.compose.ui.text.input.VisualTransformation.None
                        else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passVisible = !passVisible }) {
                                Text(if (passVisible) "隐藏" else "显示")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.webDavConnect() },
                        enabled = !state.loading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (state.loading) "连接中…" else "连接") }
                    state.error?.let {
                        Spacer(Modifier.height(10.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "支持 http/https 的 WebDAV 服务器（Nextcloud、Alist、群晖等）。账号密码只在本次连接使用，地址和用户名会记住。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                if (state.loading) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (state.busyText.isNotBlank()) {
                    Text(
                        state.busyText,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { vm.webDavUp() }) { Text("上一级") }
                    Text(
                        state.path,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                LazyColumn(Modifier.fillMaxSize()) {
                    items(state.entries, key = { it.path }) { e ->
                        ListItem(
                            headlineContent = { Text(e.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(if (e.isDir) "文件夹" else ItemSizeEngine.format(e.size))
                            },
                            leadingContent = {
                                Icon(
                                    if (e.isDir) Icons.Default.Folder else Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable { vm.webDavOpenEntry(e) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/** 打开系统文件选择器（多选）用于上传 */
fun MainViewModel.webDavUploadOpen(
    launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>
) {
    launcher.launch(arrayOf("*/*"))
}
