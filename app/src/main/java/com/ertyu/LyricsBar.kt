package com.ertyu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ertyu.engine.LrcParser

/**
 * 同名 .lrc 歌词：显示当前行前后各一行，点任意行跳转。
 * 没有歌词文件时什么都不渲染。
 */
@Composable
fun LyricsBar(audioPath: String, positionMs: Long, onSeek: (Long) -> Unit) {
    val lines = remember(audioPath) { LrcParser.load(audioPath) }
    if (lines.isEmpty()) return

    val currentIndex = remember(positionMs, lines.size) {
        var idx = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs + 120) idx = i else break
        }
        idx
    }
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex) {
        if (currentIndex >= 0) {
            listState.animateScrollToItem((currentIndex - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxWidth()) {
            itemsIndexed(lines) { index, line ->
                val active = index == currentIndex
                Text(
                    text = line.text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSeek(line.timeMs) }
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = if (active) MaterialTheme.typography.bodyLarge
                    else MaterialTheme.typography.bodySmall,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
