package com.ertyu

import com.ertyu.model.RecycleBinState
import kotlinx.coroutines.flow.MutableStateFlow

object RecycleBinStateHolder {
    val state = MutableStateFlow(RecycleBinState())
}