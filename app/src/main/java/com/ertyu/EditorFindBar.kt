package com.ertyu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.ertyu.model.EditorState

@Composable
fun EditorFindBar(vm: MainViewModel, state: EditorState) {
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

fun lineOfText(text: String, offset: Int): Int {
    if (offset <= 0) return 1
    val safe = offset.coerceAtMost(text.length)
    var line = 1
    for (i in 0 until safe) {
        if (text[i] == '\n') line++
    }
    return line
}

fun columnOfText(text: String, offset: Int): Int {
    if (offset <= 0) return 1
    val safe = offset.coerceAtMost(text.length)
    var lastNl = -1
    for (i in 0 until safe) {
        if (text[i] == '\n') lastNl = i
    }
    return safe - lastNl
}