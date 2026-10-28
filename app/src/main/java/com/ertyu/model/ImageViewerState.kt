package com.ertyu.model

import android.net.Uri

data class ImageViewerState(
    val isOpen: Boolean = false,
    val uri: Uri? = null,
    val fileName: String = ""
)