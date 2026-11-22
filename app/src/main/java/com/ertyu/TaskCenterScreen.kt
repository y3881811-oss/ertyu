package com.ertyu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.TaskCenterHolder
import com.ertyu.model.TaskItem
import com.ertyu.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCenterScreen(vm: MainViewModel) {
    val tasks by TaskCenterHolder.state.collectAsState()
    val paused by TaskCenterHolder.paused.collectAsState()
    val hasPausable = tasks.any { it.status == TaskStatus.RUNNING && it.pausable }
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    BackHandler { vm.closeTaskCenter() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务中心") },
                navigationIcon = {
                    IconButton(onClick = { vm.closeTaskCenter() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (hasPausable) {
                        TextButton(onClick = { vm.toggleTransferPause() }) {
                            Text(if (paused) "继续" else "暂停")
                        }
                    }
                    if (tasks.any { it.status != TaskStatus.RUNNING }) {
                        TextButton(onClick = { vm.dismissFinishedTasks() }) { Text("清空已完成") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(tasks, key = { it.id }) { t ->
                        TaskRow(t, fmt, onCancel = { vm.cancelTask(t.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(t: TaskItem, fmt: SimpleDateFormat, onCancel: () -> Unit) {
    val color = when (t.status) {
        TaskStatus.RUNNING -> MaterialTheme.colorScheme.primary
        TaskStatus.DONE -> MaterialTheme.colorScheme.tertiary
        TaskStatus.FAILED -> MaterialTheme.colorScheme.error
        TaskStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = t.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = t.status.label,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            if (t.status == TaskStatus.RUNNING) {
                TextButton(onClick = onCancel) { Text("取消") }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = buildString {
                append(t.kind)
                append(" · ")
                append(fmt.format(Date(t.startedAt)))
                if (t.detail.isNotBlank()) {
                    append(" · ")
                    append(t.detail)
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        val p = t.progress
        if (t.status == TaskStatus.RUNNING && p != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { p.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}
