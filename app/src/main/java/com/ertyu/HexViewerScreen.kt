package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val BYTES_PER_LINE = 16

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HexViewerScreen(vm: MainViewModel) {
    val state by vm.hexViewerState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            state.fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "已加载 ${state.loadedBytes} / ${state.fileSize} 字节",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.closeHexViewer() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                state.isLoading && state.loadedBytes == 0 -> Box(
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
                else -> HexContent(vm, state.bytes, state.fileSize, state.loadedBytes)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HexContent(
    vm: MainViewModel,
    bytes: ByteArray,
    totalSize: Long,
    loadedBytes: Int
) {
    val lineCount = (bytes.size + BYTES_PER_LINE - 1) / BYTES_PER_LINE
    val canLoadMore = loadedBytes < totalSize

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = (0 until lineCount).toList(),
            key = { it }
        ) { lineIdx ->
            HexLine(
                offset = lineIdx * BYTES_PER_LINE,
                bytes = bytes,
                lineIdx = lineIdx
            )
        }

        if (canLoadMore) {
            item(key = "load_more") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { vm.loadMoreHex() }) {
                        Text("加载更多…")
                    }
                }
            }
        }
    }
}

@Composable
private fun HexLine(offset: Int, bytes: ByteArray, lineIdx: Int) {
    val start = lineIdx * BYTES_PER_LINE
    val end = minOf(start + BYTES_PER_LINE, bytes.size)
    val lineBytes = bytes.copyOfRange(start, end)

    // 十六进制部分
    val hexPart = buildString {
        for (i in 0 until BYTES_PER_LINE) {
            if (i < lineBytes.size) {
                append("%02X".format(lineBytes[i]))
            } else {
                append("  ")
            }
            if (i == 7) append("  ") else append(" ")
        }
    }

    // ASCII 部分
    val asciiPart = buildString {
        for (b in lineBytes) {
            val c = b.toInt() and 0xFF
            append(if (c in 0x20..0x7E) c.toChar() else '.')
        }
    }

    val bg = if (lineIdx % 2 == 0) Color.Transparent
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 1.dp)
    ) {
        // 偏移
        Text(
            text = "%08X".format(offset),
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(6.dp))
        // Hex
        Text(
            text = hexPart,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            modifier = Modifier.width(360.dp)
        )
        Spacer(Modifier.width(6.dp))
        // ASCII
        Text(
            text = asciiPart,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}