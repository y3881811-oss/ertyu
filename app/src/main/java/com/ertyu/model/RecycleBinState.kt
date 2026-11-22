package com.ertyu.model

data class RecycleBinItem(
    val id: String,
    val originalPath: String,
    val name: String,
    val isDirectory: Boolean,
    val deleteTime: Long,
    val size: Long
)

data class RecycleBinState(
    val visible: Boolean = false,
    val items: List<RecycleBinItem> = emptyList(),
    val isLoading: Boolean = false,
    val selected: Set<String> = emptySet(),
    val working: Boolean = false,
    val error: String? = null
) {
    val totalBytes: Long get() = items.sumOf { it.size }

    fun selectedItems(): List<RecycleBinItem> = items.filter { it.id in selected }

    fun selectedBytes(): Long = selectedItems().sumOf { it.size }
}
