package com.ertyu.model

data class LogState(
    val visible: Boolean = false,
    val logFiles: List<LogFile> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
)

data class LogFile(
    val name: String,
    val path: String,
    val size: String,
    val time: String
)