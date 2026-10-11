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
    // 以下字段在 engine 读取时预计算，UI 直接用
    val uriString: String = uri.toString(),
    val path: String = "",
    val sizeText: String = "",
    val typeText: String = "",
    val lastModifiedText: String = ""
)