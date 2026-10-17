package com.ertyu

import com.ertyu.model.ArchiveViewerState
import kotlinx.coroutines.flow.MutableStateFlow

object ArchiveViewerStateHolder {
    val state = MutableStateFlow(ArchiveViewerState())
}