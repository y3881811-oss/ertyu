package com.ertyu.model

data class ZipProgress(
    val visible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
    val error: String? = null,
    // 新增：密码相关
    val needsPassword: Boolean = false,
    val pendingFileItem: FileItem? = null,
    val pendingIsLeft: Boolean = true,
    val pendingTargetIsLeft: Boolean = true // 用于区分是解压到当前还是另一侧
)

data class ZipPrompt(
    val item: FileItem? = null
)