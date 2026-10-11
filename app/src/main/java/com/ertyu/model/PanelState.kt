package com.ertyu.model

import android.net.Uri

data class PanelState(
    val currentUri: Uri? = null,
    val rawItems: List<FileItem> = emptyList(),   // 从磁盘读到的全部（未过滤）
    val items: List<FileItem> = emptyList(),      // 过滤 + 排序后用于显示
    val isLoading: Boolean = false,
    val error: String? = null,
    val canGoUp: Boolean = false,
    val sortMode: SortMode = SortMode.NAME_ASC,
    val showHidden: Boolean = false,
    val selectedUris: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false
)