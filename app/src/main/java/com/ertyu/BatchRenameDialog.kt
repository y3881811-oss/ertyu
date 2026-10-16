package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.model.NumberPosition

@Composable
fun BatchRenameDialog(vm: MainViewModel) {
    val state by BatchToolsState.batchRenameState.collectAsState()
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { vm.closeBatchRename() },
        title = { Text("批量重命名（${state.targets.size} 个文件）") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 前缀 / 后缀
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.prefix,
                        onValueChange = { v ->
                            vm.updateBatchRename { it.copy(prefix = v) }
                        },
                        label = { Text("前缀") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = state.suffix,
                        onValueChange = { v ->
                            vm.updateBatchRename { it.copy(suffix = v) }
                        },
                        label = { Text("后缀") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 查找 / 替换
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = state.findText,
                        onValueChange = { v ->
                            vm.updateBatchRename { it.copy(findText = v) }
                        },
                        label = { Text("查找") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = state.replaceText,
                        onValueChange = { v ->
                            vm.updateBatchRename { it.copy(replaceText = v) }
                        },
                        label = { Text("替换为") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // 正则选项
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.useRegex,
                        onCheckedChange = { v ->
                            vm.updateBatchRename { it.copy(useRegex = v) }
                        }
                    )
                    Text("使用正则表达式")
                }

                Spacer(Modifier.height(8.dp))

                // 编号
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.useNumbering,
                        onCheckedChange = { v ->
                            vm.updateBatchRename { it.copy(useNumbering = v) }
                        }
                    )
                    Text("自动编号")
                }

                if (state.useNumbering) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.numberStart.toString(),
                            onValueChange = { v ->
                                val n = v.toIntOrNull() ?: 1
                                vm.updateBatchRename { it.copy(numberStart = n) }
                            },
                            label = { Text("起始") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.numberStep.toString(),
                            onValueChange = { v ->
                                val n = v.toIntOrNull() ?: 1
                                vm.updateBatchRename { it.copy(numberStep = n) }
                            },
                            label = { Text("步长") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = state.numberDigits.toString(),
                            onValueChange = { v ->
                                val n = (v.toIntOrNull() ?: 2).coerceIn(1, 8)
                                vm.updateBatchRename { it.copy(numberDigits = n) }
                            },
                            label = { Text("位数") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("位置：", style = MaterialTheme.typography.bodySmall)
                        NumberPosition.entries.forEach { pos ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = state.numberPosition == pos,
                                    onCheckedChange = {
                                        vm.updateBatchRename {
                                            it.copy(numberPosition = pos)
                                        }
                                    }
                                )
                                Text(pos.label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text("预览：", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(4.dp))

                state.previews.take(10).forEach { p ->
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = p.oldName,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "→ ${p.newName}",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (state.previews.size > 10) {
                    Text(
                        "... 共 ${state.previews.size} 个",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.executeBatchRename() },
                enabled = state.canExecute
            ) {
                Text("执行")
            }
        },
        dismissButton = {
            TextButton(onClick = { vm.closeBatchRename() }) {
                Text("取消")
            }
        }
    )
}