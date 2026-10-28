package com.ertyu

import com.ertyu.model.PanelState
import kotlinx.coroutines.flow.MutableStateFlow

object TabManager {
    val leftTabList = MutableStateFlow<List<PanelState>>(listOf(PanelState()))
    val leftActiveIndex = MutableStateFlow(0)

    val rightTabList = MutableStateFlow<List<PanelState>>(listOf(PanelState()))
    val rightActiveIndex = MutableStateFlow(0)
}