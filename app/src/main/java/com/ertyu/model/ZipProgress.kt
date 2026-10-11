package com.ertyu.model

import android.net.Uri

data class ZipProgress(
    val visible: Boolean = false,
    val title: String = "",
    val message: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val done: Boolean = false,
    val error: String? = null
)

/** 点击 zip 时询问用户要做什么 */
data class ZipPrompt(
    val item: FileItem? = null
)