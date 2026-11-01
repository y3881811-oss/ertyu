package com.ertyu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable // 👈 新增注解
data class FileItem(
    val name: String,
    val uriString: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val path: String?,
    val size: Long = 0L,
    val typeText: String = "",
    val sizeText: String = "",
    val lastModifiedText: String = ""
)