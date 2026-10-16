package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ApkCloneDialog(vm: MainViewModel) {
    val state by ApkToolsState.cloneState.collectAsState()
    if (!state.visible) return

    AlertDialog(
        onDismissRequest = { vm.closeApkClone() },
        title = { Text("APK 共存/克隆") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    "原包名：",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    state.originalPackage.ifEmpty { "读取中…" },
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "新包名（必须与原包名等长）",
                    style = MaterialTheme.typography.labelMedium
                )
                OutlinedTextField(
                    value = state.newPackage,
                    onValueChange = { v ->
                        vm.updateApkClone {
                            it.copy(newPackage = v, error = null)
                        }
                    },
                    placeholder = {
                        Text(
                            "长度必须为 ${state.originalPackage.length} 字符"
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val hint = when {
                    state.newPackage.isEmpty() -> ""
                    state.newPackage.length != state.originalPackage.length ->
                        "当前长度 ${state.newPackage.length}，" +
                                "需要 ${state.originalPackage.length}"
                    state.newPackage == state.originalPackage ->
                        "不能与原包名相同"
                    else -> "长度匹配 ✓"
                }
                if (hint.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.lengthMatches &&
                            state.newPackage != state.originalPackage
                        ) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "需要 keystore 重新签名。请先在" +
                            "「APK 签名」页面准备 keystore，" +
                            "或直接取消。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (state.isProcessing) {
                    Spacer(Modifier.height(12.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.progressMessage,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "错误：$it",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                state.resultPath.takeIf { it.isNotEmpty() }?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "输出：$it",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // 简单实现：使用临时 keystore，这里直接告诉用户先签名
                    // 实际项目里应该弹 keystore 选择器
                    vm.closeApkClone()
                }
            ) {
                Text("关闭")
            }
        }
    )
}