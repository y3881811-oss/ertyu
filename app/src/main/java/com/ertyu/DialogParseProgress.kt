package com.ertyu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun DialogParseProgress(vm: MainViewModel) {
    val state by vm.parseProgressState.collectAsState()
    
    if (state.visible) {
        // 注意：不提供 onDismissRequest，因为解析中不允许用户点击外部取消
        AlertDialog(
            onDismissRequest = { },
            title = { Text(state.title) },
            text = {
                Column {
                    Text(state.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = { }
        )
    }
}