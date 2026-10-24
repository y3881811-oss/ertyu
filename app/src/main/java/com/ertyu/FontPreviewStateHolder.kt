package com.ertyu

import com.ertyu.model.FontPreviewState
import kotlinx.coroutines.flow.MutableStateFlow

object FontPreviewStateHolder {
    val state = MutableStateFlow(FontPreviewState())
}