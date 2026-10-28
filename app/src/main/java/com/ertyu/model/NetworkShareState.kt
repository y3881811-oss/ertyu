package com.ertyu.model

data class NetworkShareState(
    val visible: Boolean = false,
    val isRunning: Boolean = false,
    
    // FTP 配置
    val ftpPort: Int = 2121,
    val ftpUsername: String = "mtfm",
    val ftpPassword: String = "mtfm123",
    
    // HTTP 快传配置
    val httpPort: Int = 7333,
    
    // 共享目录
    val sharePath: String = "",
    
    val ipAddress: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)