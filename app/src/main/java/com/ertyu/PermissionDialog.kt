package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.engine.PermissionManager
import com.ertyu.model.PermissionHolder

@Composable
fun PermissionDialog(vm: MainViewModel) {
    val target by PermissionHolder.target.collectAsState()
    val current by PermissionHolder.currentMode.collectAsState()
    val t = target ?: return

    // 三个八进制位，逐位勾选
    var owner by remember(t.path) { mutableIntStateOf(6) }
    var group by remember(t.path) { mutableIntStateOf(4) }
    var other by remember(t.path) { mutableIntStateOf(4) }
    var manual by remember(t.path) { mutableStateOf("") }

    // 读到当前权限后同步到勾选
    LaunchedEffect(current) {
        val m = current
        if (m.length >= 3) {
            val digits = m.takeLast(3)
            owner = digits[0] - '0'
            group = digits[1] - '0'
            other = digits[2] - '0'
        }
    }

    val octal = "$owner$group$other"
    val effective = manual.ifBlank { octal }

    AlertDialog(
        onDismissRequest = { vm.closePermissionDialog() },
        title = { Text("修改权限") },
        text = {
            Column {
                Text(
                    t.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    t.path,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前", style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        current.ifBlank { "未知（需 root 读取）" },
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(8.dp))
                PermGroup("所有者", owner) { owner = it }
                PermGroup("同组", group) { group = it }
                PermGroup("其他", other) { other = it }

                Spacer(Modifier.height(6.dp))
                Text(
                    "chmod $octal  →  ${PermissionManager.describe(octal)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it.filter { c -> c in '0'..'7' }.take(4) },
                    label = { Text("直接输入八进制（可留空）") },
                    placeholder = { Text("644 / 755") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ 改错系统文件权限可能导致 App 崩溃甚至系统异常；/sdcard 上 chmod 无效。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { vm.applyPermission(effective) },
                enabled = PermissionManager.isValidMode(effective)
            ) { Text("应用 $effective", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = { vm.closePermissionDialog() }) { Text("取消") }
        }
    )
}

@Composable
private fun PermGroup(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(52.dp))
        listOf(4 to "r", 2 to "w", 1 to "x").forEach { (bit, name) ->
            val checked = value and bit != 0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onChange(if (checked) value and bit.inv() else value or bit) }
                    .padding(end = 10.dp)
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onChange(if (checked) value and bit.inv() else value or bit) }
                )
                Text(name, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
            }
        }
        Text("$value", fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.labelMedium)
    }
}
