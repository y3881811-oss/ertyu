package com.ertyu.model

import android.net.Uri

data class PanelState(
    val currentUri: Uri? = null,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canGoUp: Boolean = false
)