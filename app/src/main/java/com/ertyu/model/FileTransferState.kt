package com.ertyu.model

data class FileTransferState(
    val visible: Boolean = false,
    val title: String = "",
    val currentFile: String = "",
    val copiedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedText: String = "",
    val isDone: Boolean = false,
    val error: String? = null
) {
    val progress: Float
        get() = if (totalBytes <= 0) 0f else (copiedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
}

data class TransferProgress(
    val copied: Long,
    val total: Long,
    val currentFileName: String,
    val speed: Long
)