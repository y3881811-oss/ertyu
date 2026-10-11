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
    // 预计算，避免每帧调用 uri.toString()
    val uriString: String = uri.toString()
)