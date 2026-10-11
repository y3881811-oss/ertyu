package com.ertyu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ertyu.model.EditorState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(vm: MainViewModel) {
    val state by vm.editor.collectAsState()

    var showDiscardDialog by remember { mutableStateOf(false) }
    var jumpLineText by remember { mutableStateOf("") }

    // TextFieldValue 保持光标位置
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(state.content))
    }

    // 外部修改（查找替换）同步到 fieldValue
    LaunchedEffect(state.content) {
        if (fieldValue.text != state.content) {
            fieldValue = fieldValue.copy(text = state.content)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.fileName + if (state.isDirty) " *" else "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.isDirty) showDiscardDialog = true
                        else vm.closeEditor()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { vm.openFind() }) {
                        Icon(Icons.Default.Search, contentDescription = "查找")
                    }
                    IconButton(onClick = { vm.decreaseFontSize() }) {
                        Icon(Icons.Default.Remove, contentDescription = "缩小")
                    }
                    IconButton(onClick = { vm.increaseFontSize() }) {
                        Icon(Icons.Default.Add, contentDescription = "放大")
                    }
                    IconButton(
                        onClick = { vm.saveEditor() },
                        enabled = state.isDirty && !state.isLoading
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.findVisible) {
                FindReplaceBar(vm, state)
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.isLoading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "错误: ${state.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    else -> {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                                .padding(12.dp)
                        ) {
                            BasicTextField(
                                value = fieldValue,
                                onValueChange = { tv ->
                                    fieldValue = tv
                                    vm.updateEditorContent(tv.text)
                                    val offset = tv.selection.start
                                        .coerceIn(0, tv.text.length)
                                    vm.updateCursorPosition(
                                        lineOf(tv.text, offset),
                                        columnOf(tv.text, offset)
                                    )
                                },
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = state.fontSize.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(
                                    MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // 状态栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "行 ${state.cursorLine}, 列 ${state.cursorColumn}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = { vm.openJumpDialog() },
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("跳转", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${state.totalLines} 行 · ${state.totalChars} 字符",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃修改？") },
            text = { Text("文件有未保存的修改，确定要退出吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    vm.closeEditor()
                }) { Text("放弃") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    if (state.jumpDialogVisible) {
        AlertDialog(
            onDismissRequest = { vm.closeJumpDialog() },
            title = { Text("跳转到行") },
            text = {
                OutlinedTextField(
                    value = jumpLineText,
                    onValueChange = {
                        jumpLineText = it.filter { c -> c.isDigit() }
                    },
                    placeholder = {
                        Text("输入行号（1-${state.totalLines}）")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val line = jumpLineText.toIntOrNull()
                    if (line != null && line >= 1 && line <= state.totalLines) {
                        val offset = vm.lineToOffset(line)
                        val newText = state.content
                        fieldValue = TextFieldValue(
                            text = newText,
                            selection = androidx.compose.ui.text.TextRange(
                                offset.coerceIn(0, newText.length)
                            )
                        )
                        vm.updateCursorPosition(line, 1)
                    }
                    jumpLineText = ""
                    vm.closeJumpDialog()
                }) { Text("跳转") }
            },
            dismissButton = {
                TextButton(onClick = {
                    jumpLineText = ""
                    vm.closeJumpDialog()
                }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun FindReplaceBar(vm: MainViewModel, state: EditorState) {
    var findText by remember { mutableStateOf(state.findQuery) }
    var replaceText by remember { mutableStateOf(state.replaceQuery) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = findText,
                onValueChange = {
                    findText = it
                    vm.updateFindQuery(it)
                },
                placeholder = { Text("查找") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (state.totalMatches > 0)
                    "${state.currentMatchIndex}/${state.totalMatches}"
                else "0/0",
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace
            )
            IconButton(onClick = { vm.findPrev() }) {
                Text("↑", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = { vm.findNext() }) {
                Text("↓", style = MaterialTheme.typography.titleMedium)
            }
            IconButton(onClick = { vm.closeFind() }) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = replaceText,
                onValueChange = {
                    replaceText = it
                    vm.updateReplaceQuery(it)
                },
                placeholder = { Text("替换为") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { vm.replaceCurrent() }) {
                Text("替换")
            }
            TextButton(onClick = { vm.replaceAll() }) {
                Text("全部")
            }
        }
    }
}

private fun lineOf(text: String, offset: Int): Int {
    if (offset <= 0) return 1
    val safe = offset.coerceAtMost(text.length)
    var line = 1
    for (i in 0 until safe) {
        if (text[i] == '\n') line++
    }
    return line
}

private fun columnOf(text: String, offset: Int): Int {
    if (offset <= 0) return 1
    val safe = offset.coerceAtMost(text.length)
    var lastNl = -1
    for (i in 0 until safe) {
        if (text[i] == '\n') lastNl = i
    }
    return safe - lastNl
}