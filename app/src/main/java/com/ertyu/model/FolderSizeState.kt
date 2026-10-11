package com.ertyu.model

data class FolderSizeState(
    val isCalculating: Boolean = false,
    val scanned: Int = 0,
    val totalBytes: Long = 0,
    val done: Boolean = false,
    val error: String? = null
)