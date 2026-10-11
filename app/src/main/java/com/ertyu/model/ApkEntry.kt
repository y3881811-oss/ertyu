package com.ertyu.model

data class ApkEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val sizeText: String,
    val compressedText: String,
    val depth: Int,
    val isDex: Boolean
)

data class ApkBrowserState(
    val visible: Boolean = false,
    val apkPath: String = "",
    val apkName: String = "",
    val entries: List<ApkEntry> = emptyList(),
    val filterQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredEntries: List<ApkEntry>
        get() = if (filterQuery.isBlank()) entries
        else entries.filter { it.path.contains(filterQuery, ignoreCase = true) }
}