package com.ertyu

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.TaskCenterHolder
import com.ertyu.model.TaskItem
import com.ertyu.model.TaskStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TaskFilter(val label: String) {
    ALL("全部"),
    RUNNING("进行中"),
    FAILED("失败"),
    FINISHED("已结束")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskCenterScreen(vm: MainViewModel) {
    val tasks by TaskCenterHolder.state.collectAsState()
    val paused by TaskCenterHolder.paused.collectAsState()
    val context = LocalContext.current
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    var filter by remember { mutableStateOf(TaskFilter.ALL) }
    var newestFirst by remember { mutableStateOf(true) }
    var confirmClear by remember { mutableStateOf(false) }

    val hasPausable = tasks.any { it.status == TaskStatus.RUNNING && it.pausable }
    val runningCount = tasks.count { it.status == TaskStatus.RUNNING }
    val failedCount = tasks.count { it.status == TaskStatus.FAILED }
    val finishedCount = tasks.count { it.status != TaskStatus.RUNNING }

    val shown = remember(tasks, filter, newestFirst) {
        val base = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.RUNNING -> tasks.filter { it.status == TaskStatus.RUNNING }
            TaskFilter.FAILED -> tasks.filter { it.status == TaskStatus.FAILED }
            TaskFilter.FINISHED -> tasks.filter { it.status != TaskStatus.RUNNING }
        }
        if (newestFirst) base else base.reversed()
    }

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
                    IconButton(onClick = { newestFirst = !newestFirst }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = if (newestFirst) "按时间倒序" else "按时间正序"
                        )
                    }
                    if (hasPausable) {
                        TextButton(onClick = { vm.toggleTransferPause() }) {
                            Text(if (paused) "继续" else "暂停")
                        }
                    }
                    if (finishedCount > 0) {
                        TextButton(onClick = { confirmClear = true }) { Text("清理") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TaskFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = {
                            Text(
                                when (f) {
                                    TaskFilter.ALL -> "${f.label} ${tasks.size}"
                                    TaskFilter.RUNNING -> "${f.label} $runningCount"
                                    TaskFilter.FAILED -> "${f.label} $failedCount"
                                    TaskFilter.FINISHED -> "${f.label} $finishedCount"
                                }
                            )
                        }
                    )
                }
            }

            if (tasks.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("还没有任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (shown.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("这个分类下没有任务", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(shown, key = { it.id }) { t ->
                        TaskRow(
                            t = t,
                            fmt = fmt,
                            onCancel = { vm.cancelTask(t.id) },
                            onCopyError = { copyTaskError(context, t) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清理任务记录") },
            text = {
                Column {
                    Text("将移除 $finishedCount 条已结束的记录（进行中的任务不受影响）。")
                    if (failedCount > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "其中包含 $failedCount 条失败记录，清理后错误详情就查不到了；" +
                                "需要留档请先「复制错误」。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.dismissFinishedTasks()
                }) { Text("清理") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TaskRow(
    t: TaskItem,
    fmt: SimpleDateFormat,
    onCancel: () -> Unit,
    onCopyError: () -> Unit
) {
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
            } else if (t.status == TaskStatus.FAILED) {
                TextButton(onClick = onCopyError) { Text("复制错误") }
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
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        val p = t.progress
        if (t.status == TaskStatus.RUNNING && p != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(progress = { p.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun copyTaskError(context: Context, t: TaskItem) {
    try {
        val text = buildString {
            append("【${t.kind}】${t.title}")
            append("\n开始：")
            append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(t.startedAt)))
            if (t.detail.isNotBlank()) {
                append("\n详情：")
                append(t.detail)
            }
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("任务错误", text))
    } catch (_: Exception) {
    }
}
