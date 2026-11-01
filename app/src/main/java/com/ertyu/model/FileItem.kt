package com.ertyu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable // 👈 加上这个，Compose 就能确认它是不可变的，优化重组
data class FileItem(
    val name: String,
    val uriString: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val path: String?,
    val size: Long = 0L,
    val typeText: String = "",
    val sizeText: String = ""
)