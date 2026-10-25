package com.ertyu

import com.ertyu.model.LogState
import kotlinx.coroutines.flow.MutableStateFlow

object LogStateHolder {
    val state = MutableStateFlow(LogState())
}