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
    val uriString: String = uri.toString(),
    val sizeText: String = ""
)