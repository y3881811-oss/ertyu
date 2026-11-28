package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class BatchMkdirState(
    val visible: Boolean = false,
    val isLeft: Boolean = true,
    val creating: Boolean = false
)

object BatchMkdirHolder {
    val state = MutableStateFlow(BatchMkdirState())
}
