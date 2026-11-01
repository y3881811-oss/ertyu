package com.ertyu.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable // 👈 保留这个注解，告诉 Compose 它是不可变的，提升滑动性能
data class FileItem(
    val name: String,
    val uriString: String,
    val uri: Uri,
    val isDirectory: Boolean,
    val path: String,
    val size: Long = 0L,
    // 👇 以下字段必须加回来，否则底层引擎和属性弹窗会报错
    val lastModified: Long = 0L,
    val mimeType: String? = null,
    val sizeText: String = "",
    val lastModifiedText: String = "",
    val childCount: Int = -1,
    val typeText: String = ""
)