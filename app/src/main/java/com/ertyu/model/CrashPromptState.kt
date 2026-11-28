package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class CrashPromptState(
    val visible: Boolean = false,
    val timeText: String = "",
    val summary: String = "",
    val logPath: String = ""
)

object CrashPromptHolder {
    val state = MutableStateFlow(CrashPromptState())
}
