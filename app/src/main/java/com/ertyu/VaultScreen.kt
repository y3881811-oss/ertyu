package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.VaultHolder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(vm: MainViewModel) {
    val state by VaultHolder.state.collectAsState()
    var pwd by remember { mutableStateOf("") }
    var pwd2 by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var confirmDestroy by remember { mutableStateOf(false) }

    BackHandler { vm.closeVault() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("隐私空间") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeVault() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.unlocked) {
                        IconButton(onClick = { vm.lockVault() }) {
                            Icon(Icons.Default.Lock, contentDescription = "锁定")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("清空隐私空间", color = MaterialTheme.colorScheme.error) },
                                    onClick = { showMenu = false; confirmDestroy = true }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            if (state.busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text(
                    state.busyText,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            when {
                !state.hasPassword -> {
                    // 首次创建
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("设置访问密码", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "文件会用 AES-256-GCM 加密后存放。密码只用于本地解密，不会上传。\n" +
                                "⚠️ 忘记密码 = 数据永久无法找回，App 重置或换机也会丢失。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pwd, onValueChange = { pwd = it },
                            label = { Text("密码（至少 4 位）") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = pwd2, onValueChange = { pwd2 = it },
                            label = { Text("确认密码") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { if (vm.createVault(pwd, pwd2)) { pwd = ""; pwd2 = "" } },
                            enabled = pwd.length >= 4 && pwd == pwd2,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("创建隐私空间") }
                    }
                }

                !state.unlocked -> {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                        Icon(
                            Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("输入密码解锁", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = pwd, onValueChange = { pwd = it },
                            label = { Text("密码") },
                            singleLine = true,
                            isError = state.error != null,
                            supportingText = { state.error?.let { Text(it) } },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.unlockVault(pwd); pwd = "" },
                            enabled = pwd.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("解锁") }
                    }
                }

                else -> {
                    if (state.entries.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "还没有文件\n在文件列表选中后长按 → 移入隐私空间",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(state.entries, key = { it.id }) { entry ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            entry.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            (if (entry.isDir) "文件夹 · " else "") +
                                                ItemSizeEngine.format(entry.size) + " · " +
                                                SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
                                                    .format(Date(entry.mtime)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    IconButton(onClick = { vm.exportFromVault(entry) }) {
                                        Icon(Icons.Default.Upload, contentDescription = "移出")
                                    }
                                    IconButton(onClick = { vm.deleteVaultEntry(entry) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDestroy) {
        AlertDialog(
            onDismissRequest = { confirmDestroy = false },
            title = { Text("清空隐私空间") },
            text = { Text("将删除 ${state.entries.size} 项，且**无法恢复**（文件本来就没有其他副本）。确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDestroy = false
                    vm.destroyVault()
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDestroy = false }) { Text("取消") }
            }
        )
    }
}
