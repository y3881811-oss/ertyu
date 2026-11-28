package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ertyu.model.DragDropHolder

@Composable
fun DragMoveConfirmDialog(vm: MainViewModel) {
    val pending by DragDropHolder.pending.collectAsState()
    val p = pending ?: return

    AlertDialog(
        onDismissRequest = { vm.cancelDragMove() },
        title = { Text("移动 ${p.uris.size} 项到${if (p.fromLeft) "右侧" else "左侧"}？") },
        text = {
            Column {
                Text(
                    p.names.take(3).joinToString("\n") +
                        if (p.names.size > 3) "\n…等 ${p.names.size} 项" else ""
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "将移动到${if (p.fromLeft) "右" else "左"}面板当前目录；同名文件会自动改名，不会覆盖。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { vm.confirmDragMove() }) { Text("移动") }
        },
        dismissButton = {
            TextButton(onClick = { vm.cancelDragMove() }) { Text("取消") }
        }
    )
}
