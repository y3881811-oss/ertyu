package com.ertyu.model

data class FileClipboardState(
    val items: List<FileItem> = emptyList(),
    val isCut: Boolean = false,
    val sourceIsLeft: Boolean = false
) {
    val isEmpty: Boolean get() = items.isEmpty()
}