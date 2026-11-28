package com.ertyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ertyu.model.ManifestHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManifestViewerScreen(vm: MainViewModel) {
    val state by ManifestHolder.state.collectAsState()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }

    BackHandler { vm.closeManifestViewer() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manifest · ${state.apkName.take(24)}") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeManifestViewer() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        enabled = state.xml.isNotBlank(),
                        onClick = {
                            copyText(context, state.xml)
                            vm.showToast("XML 已复制")
                        }
                    ) { Text("复制") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            state.error?.let {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                return@Column
            }
            if (!state.loading && state.xml.isNotBlank()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (state.packageName.isBlank()) "" else "包名 ${state.packageName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "权限 ${state.permissions.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                PrimaryTabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("XML") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("权限") })
                }
                if (tab == 0) {
                    Text(
                        text = state.xml,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)
                    )
                } else {
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        if (state.permissions.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("该 APK 未声明权限", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            state.permissions.forEach { p ->
                                Text(
                                    text = p,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun copyText(context: Context, text: String) {
    try {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("manifest", text))
    } catch (_: Exception) { }
}
