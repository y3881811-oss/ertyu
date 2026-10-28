package com.ertyu

import com.ertyu.model.GalleryState
import kotlinx.coroutines.flow.MutableStateFlow

object GalleryStateHolder {
    val state = MutableStateFlow(GalleryState())
}