package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class SafeModeState(
    val visible: Boolean = false,
    val crashCount: Int = 0
)

object SafeModeHolder {
    val state = MutableStateFlow(SafeModeState())
}
