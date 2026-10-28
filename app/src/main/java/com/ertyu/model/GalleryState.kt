package com.ertyu.model

data class GalleryState(
    val visible: Boolean = false,
    val images: List<FileItem> = emptyList(),
    val initialIndex: Int = 0
)