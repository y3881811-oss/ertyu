package com.ertyu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ertyu.model.DiffLine
import com.ertyu.model.DiffLineType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiffScreen(vm: MainViewModel) {
    val state by DiffStateHolder.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("文件对比", maxLines = 1)
                        if (!state.isLoading && state.error == null) {
                            Text(
                                "+${state.addedCount}  -${state.removedCount}  " +
                                        "~${state.modifiedCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeDiff() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
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
            // 左右文件名
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "← ${state.leftName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "→ ${state.rightName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider()

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
                        "错误：${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(state.lines) { line ->
                        DiffLineRow(line)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLineRow(line: DiffLine) {
    val bg = when (line.type) {
        DiffLineType.SAME -> Color.Transparent
        DiffLineType.ADDED ->
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        DiffLineType.REMOVED ->
            MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
        DiffLineType.MODIFIED ->
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    }
    val prefix = when (line.type) {
        DiffLineType.SAME -> " "
        DiffLineType.ADDED -> "+"
        DiffLineType.REMOVED -> "-"
        DiffLineType.MODIFIED -> "~"
    }
    val lineColor = when (line.type) {
        DiffLineType.SAME -> MaterialTheme.colorScheme.onSurface
        DiffLineType.ADDED -> MaterialTheme.colorScheme.primary
        DiffLineType.REMOVED -> MaterialTheme.colorScheme.error
        DiffLineType.MODIFIED -> MaterialTheme.colorScheme.tertiary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    ) {
        // 左行号
        Text(
            text = line.leftLineNo?.toString() ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(36.dp)
        )
        // 右行号
        Text(
            text = line.rightLineNo?.toString() ?: "",
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            modifier = Modifier.width(36.dp)
        )
        Spacer(Modifier.width(4.dp))
        // 前缀
        Text(
            text = prefix,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = lineColor,
            maxLines = 1
        )
        Spacer(Modifier.width(4.dp))
        // 内容
        when (line.type) {
            DiffLineType.SAME -> {
                Text(
                    text = line.leftText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            DiffLineType.ADDED -> {
                Text(
                    text = line.rightText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = lineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            DiffLineType.REMOVED -> {
                Text(
                    text = line.leftText,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = lineColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            DiffLineType.MODIFIED -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = line.leftText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = line.rightText,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}