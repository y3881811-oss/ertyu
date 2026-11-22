package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.BatchInstallHolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchInstallScreen(vm: MainViewModel) {
    val state by BatchInstallHolder.state.collectAsState()

    BackHandler { vm.closeBatchInstall() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("批量安装 (${state.index}/${state.total})") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeBatchInstall() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Text(
                text = if (state.finished)
                    "全部 ${state.total} 个安装包已提交，请在系统安装界面逐个确认。"
                else
                    "系统每次安装都需要你确认一下，所以按顺序逐个提交；点下面按钮安装下一个。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(Modifier.weight(1f)) {
                items(state.items, key = { it.uriString }) { item ->
                    val idx = state.items.indexOf(item)
                    val submitted = idx < state.index
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (submitted) Icons.Default.CheckCircle else Icons.Default.Android,
                            contentDescription = null,
                            tint = if (submitted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (submitted) "已提交" else "等待安装",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = ItemSizeEngine.format(item.size),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }

            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (state.finished) {
                        TextButton(onClick = { vm.resetBatchInstall() }) { Text("重新开始") }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(onClick = { vm.installNextInQueue() }, enabled = !state.finished) {
                        Text(if (state.index == 0) "安装第 1 个" else "安装下一个")
                    }
                }
            }
        }
    }
}
