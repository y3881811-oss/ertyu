package com.ertyu.model

data class ArchiveViewerState(
    val visible: Boolean = false,
    val archivePath: String = "",
    val archiveName: String = "",
    val sourceIsLeft: Boolean = true,
    val entries: List<ArchiveEntry> = emptyList(),
    val filter: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val filteredEntries: List<ArchiveEntry>
        get() = if (filter.isBlank()) entries
        else entries.filter { it.path.contains(filter, ignoreCase = true) }
}