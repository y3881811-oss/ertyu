package com.ertyu.model

import java.io.File

data class ContentSearchState(
    val isSearching: Boolean = false,
    val results: List<ContentSearchResult> = emptyList(),
    val query: String = ""
)
data class ContentSearchResult(val file: File, val lineNum: Int, val content: String)

data class DuplicateScanState(
    val isScanning: Boolean = false,
    val progressText: String = "",
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedFiles: Set<String> = emptySet()
)
data class DuplicateGroup(val hash: String, val files: List<File>)