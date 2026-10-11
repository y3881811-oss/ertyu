package com.ertyu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class FileItem(
    val name: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val size: Long = 0,
    val lastModified: Long = 0,
    val mimeType: String? = null,
    // 预计算字段
    val uriString: String = uri.toString(),
    val path: String = "",
    val sizeText: String = "",
    val typeText: String = "",
    val lastModifiedText: String = "",
    // 文件夹内条目数（仅目录有效，-1 表示未知）
    val childCount: Int = -1
)