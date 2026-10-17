package com.ertyu

import com.ertyu.model.DiffState
import kotlinx.coroutines.flow.MutableStateFlow

object DiffStateHolder {
    val state = MutableStateFlow(DiffState())
}