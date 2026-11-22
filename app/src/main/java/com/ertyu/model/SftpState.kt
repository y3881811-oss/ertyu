package com.ertyu.model

import com.ertyu.engine.RemoteEntry
import kotlinx.coroutines.flow.MutableStateFlow

data class SftpState(
    val visible: Boolean = false,
    val connecting: Boolean = false,
    val connected: Boolean = false,
    val host: String = "",
    val port: Int = 22,
    val user: String = "",
    val path: String = "/",
    val entries: List<RemoteEntry> = emptyList(),
    val loading: Boolean = false,
    val busyText: String = "",
    val error: String? = null
)

object SftpHolder {
    val state = MutableStateFlow(SftpState())
}
