package com.ertyu.model

data class WebDavState(
    val isRunning: Boolean = false,
    val port: Int = 7326,
    val username: String = "mtfm",
    val password: String = "mtfm123",
    val sharePath: String = "",
    val serverUrl: String = "",
    val ipAddress: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)