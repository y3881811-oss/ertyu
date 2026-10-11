package com.ertyu.model

import android.net.Uri

data class EditorState(
    val isOpen: Boolean = false,
    val uri: Uri? = null,
    val fileName: String = "",
    val content: String = "",
    val originalContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    // 查找替换
    val findVisible: Boolean = false,
    val findQuery: String = "",
    val replaceQuery: String = "",
    val totalMatches: Int = 0,
    val currentMatchIndex: Int = 0,
    // 跳转
    val jumpDialogVisible: Boolean = false,
    // 字号
    val fontSize: Float = 14f,
    // 状态栏
    val cursorLine: Int = 1,
    val cursorColumn: Int = 1
) {
    val isDirty: Boolean get() = content != originalContent
    val totalLines: Int
        get() = if (content.isEmpty()) 1 else content.count { it == '\n' } + 1
    val totalChars: Int get() = content.length
}

data class ImageViewerState(
    val isOpen: Boolean = false,
    val uri: Uri? = null,
    val fileName: String = ""
)