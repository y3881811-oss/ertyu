package com.ertyu.model

import android.net.Uri

data class EditorState(
    val isOpen: Boolean = false,
    val uri: Uri? = null,
    val fileName: String = "",
    val content: String = "",
    val originalContent: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val isDirty: Boolean get() = content != originalContent
}

data class ImageViewerState(
    val isOpen: Boolean = false,
    val uri: Uri? = null,
    val fileName: String = ""
)