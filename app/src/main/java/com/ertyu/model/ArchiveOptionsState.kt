package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

// 解压选项状态
data class ExtractOptionsState(
    val visible: Boolean = false,
    val targets: List<FileItem> = emptyList(),
    val sourceIsLeft: Boolean = true,
    val folderName: String = "",
    val toOtherPanel: Boolean = false,
    val deleteSource: Boolean = false
)

// 压缩选项状态
data class CompressOptionsState(
    val visible: Boolean = false,
    val targets: List<FileItem> = emptyList(),
    val sourceIsLeft: Boolean = true,
    val fileName: String = "",
    val format: ArchiveFormat = ArchiveFormat.ZIP,
    val compressionLevel: Int = 5, // 1-9, 默认5
    val separateFolders: Boolean = false, // 单独压缩每个文件
    val deleteSource: Boolean = false,
    val toOtherPanel: Boolean = false,
    val password: String = ""   // ZIP/7z 加密密码，空 = 不加密
)

object ArchiveOptionsHolder {
    val extractState = MutableStateFlow(ExtractOptionsState())
    val compressState = MutableStateFlow(CompressOptionsState())
}