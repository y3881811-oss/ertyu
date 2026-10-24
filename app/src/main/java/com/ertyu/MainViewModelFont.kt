package com.ertyu

import com.ertyu.model.FileItem
import com.ertyu.model.FontPreviewState

fun MainViewModel.openFontPreview(item: FileItem) {
    val path = item.uri.path ?: return
    FontPreviewStateHolder.state.value = FontPreviewState(
        visible = true,
        fontPath = path,
        fontName = item.name
    )
}