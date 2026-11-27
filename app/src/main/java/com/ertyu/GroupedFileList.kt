package com.ertyu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ertyu.model.FileItem
import com.ertyu.model.PanelState

private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg")
private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")
private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus")
private val DOC_EXTS = setOf("txt", "md", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "xml", "html", "csv", "log")
private val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst")
private val APK_EXTS = setOf("apk", "apks", "xapk", "apkm")

/** 按类型分组渲染；分组内保持原有排序 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GroupedFileList(
    state: PanelState,
    items: List<FileItem>,
    folderSizeOf: (FileItem) -> String?,
    onClick: (FileItem) -> Unit,
    onLongClick: (FileItem) -> Unit,
    onMoreClick: (FileItem) -> Unit
) {
    val groups = rememberGrouped(items)

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        groups.forEach { (title, list) ->
            stickyHeader(key = "h_$title") {
                Text(
                    text = "$title（${list.size}）",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            items(list, key = { it.uriString }, contentType = { "file_list" }) { item ->
                FileRow(
                    item = item,
                    isSelected = { state.selectedUris.contains(item.uriString) },
                    isSelectionMode = state.isSelectionMode,
                    dragFromLeft = null,
                    folderSizeText = folderSizeOf(item),
                    onClick = { onClick(item) },
                    onLongClick = { onLongClick(item) },
                    onMoreClick = { onMoreClick(item) }
                )
            }
        }
    }
}

@Composable
private fun rememberGrouped(items: List<FileItem>): List<Pair<String, List<FileItem>>> {
    return androidx.compose.runtime.remember(items) {
        val order = listOf("文件夹", "图片", "视频", "音频", "文档", "压缩包", "安装包", "其他")
        val map = LinkedHashMap<String, MutableList<FileItem>>()
        order.forEach { map[it] = ArrayList() }
        items.forEach { item ->
            val key = when {
                item.isDirectory -> "文件夹"
                item.name.substringAfterLast('.', "").lowercase() in IMAGE_EXTS -> "图片"
                item.name.substringAfterLast('.', "").lowercase() in VIDEO_EXTS -> "视频"
                item.name.substringAfterLast('.', "").lowercase() in AUDIO_EXTS -> "音频"
                item.name.substringAfterLast('.', "").lowercase() in DOC_EXTS -> "文档"
                item.name.substringAfterLast('.', "").lowercase() in ARCHIVE_EXTS -> "压缩包"
                item.name.substringAfterLast('.', "").lowercase() in APK_EXTS -> "安装包"
                else -> "其他"
            }
            map.getValue(key).add(item)
        }
        order.mapNotNull { k -> map[k]?.takeIf { it.isNotEmpty() }?.let { k to it.toList() } }
    }
}
