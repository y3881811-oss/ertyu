package com.ertyu.model

data class SearchState(
    val visible: Boolean = false,
    val sourceIsLeft: Boolean = true,
    val rootPath: String = "",
    val query: String = "",
    val isSearching: Boolean = false,
    val scannedDirs: Int = 0,
    val scannedFiles: Int = 0,
    val results: List<FileItem> = emptyList(),
    val error: String? = null,
    val truncated: Boolean = false   // 结果超出上限
)