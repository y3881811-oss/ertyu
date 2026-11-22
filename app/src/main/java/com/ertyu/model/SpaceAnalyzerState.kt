package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class BigFileEntry(
    val path: String,
    val name: String,
    val size: Long
)

data class FolderUsage(
    val path: String,
    val bytes: Long,
    val fileCount: Int
)

data class SpaceAnalyzerState(
    val visible: Boolean = false,
    val root: String = "/storage/emulated/0",
    val minSizeMb: Int = 50,
    val isScanning: Boolean = false,
    val finished: Boolean = false,
    val scannedFiles: Int = 0,
    val totalBytes: Long = 0L,
    val bigFiles: List<BigFileEntry> = emptyList(),
    val topFolders: List<FolderUsage> = emptyList(),
    val error: String? = null
)

object SpaceAnalyzerHolder {
    val state = MutableStateFlow(SpaceAnalyzerState())
}
