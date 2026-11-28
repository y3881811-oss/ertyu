package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class SplitState(
    val visible: Boolean = false,
    val isMerge: Boolean = false,
    val targetPath: String = "",
    val targetName: String = "",
    val partSizeMb: Int = 50,
    val running: Boolean = false,
    val progress: Float = 0f,
    val message: String = ""
)

object SplitHolder {
    val state = MutableStateFlow(SplitState())
}
