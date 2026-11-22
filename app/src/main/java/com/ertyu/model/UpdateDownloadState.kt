package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class UpdateDownloadState(
    val visible: Boolean = false,
    val phase: String = "",
    val downloaded: Long = 0L,
    val total: Long = 0L,
    val percent: Int = 0,
    val error: String? = null,
    val done: Boolean = false,
    val paused: Boolean = false
)

object UpdateDownloadHolder {
    val state = MutableStateFlow(UpdateDownloadState())
}
