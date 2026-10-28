package com.ertyu

import com.ertyu.model.FileItem
import com.ertyu.model.GalleryState

private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")

fun MainViewModel.openGallery(isLeft: Boolean, clickedItem: FileItem) {
    val panelState = if (isLeft) _leftPanel.value else _rightPanel.value
    val images = panelState.items.filter { 
        !it.isDirectory && it.name.substringAfterLast('.', "").lowercase() in IMAGE_EXTS 
    }
    val index = images.indexOfFirst { it.uriString == clickedItem.uriString }.coerceAtLeast(0)
    
    GalleryStateHolder.state.value = GalleryState(
        visible = true,
        images = images,
        initialIndex = index
    )
}

fun MainViewModel.closeGallery() {
    GalleryStateHolder.state.value = GalleryState()
}