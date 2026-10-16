package com.ertyu.model

data class ChecksumState(
    val visible: Boolean = false,
    val fileName: String = "",
    val filePath: String = "",
    val fileSize: Long = 0,
    val isCalculating: Boolean = false,
    val progress: Float = 0f,
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
    val error: String? = null
)