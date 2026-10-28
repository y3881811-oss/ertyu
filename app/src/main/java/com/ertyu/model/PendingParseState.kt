package com.ertyu.model

data class PendingParseState(
    val visible: Boolean = false,
    val apkPath: String = "",
    val entryPath: String = "",
    val entryName: String = ""
)