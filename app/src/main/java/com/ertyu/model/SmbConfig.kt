package com.ertyu.model

data class SmbConfig(
    val host: String = "",
    val shareName: String = "",
    val username: String = "",
    val password: String = "",
    val domain: String = ""
)

data class SmbConnectState(
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)