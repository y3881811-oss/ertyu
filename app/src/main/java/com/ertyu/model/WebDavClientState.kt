package com.ertyu.model

import com.ertyu.engine.WebDavClient
import kotlinx.coroutines.flow.MutableStateFlow

data class WebDavClientState(
    val visible: Boolean = false,
    val url: String = "",
    val user: String = "",
    val pass: String = "",
    val connected: Boolean = false,
    val path: String = "/",
    val entries: List<WebDavClient.Entry> = emptyList(),
    val loading: Boolean = false,
    val busyText: String = "",
    val error: String? = null
)

object WebDavClientHolder {
    val state = MutableStateFlow(WebDavClientState())
}
