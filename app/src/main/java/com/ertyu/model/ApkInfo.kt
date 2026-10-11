package com.ertyu.model

import android.graphics.Bitmap

data class ApkInfo(
    val filePath: String,
    val fileName: String,
    val fileSize: Long,
    val fileSizeText: String,
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val permissions: List<String>,
    val activityCount: Int,
    val serviceCount: Int,
    val receiverCount: Int,
    val providerCount: Int,
    val isDebug: Boolean,
    val icon: Bitmap?
)

data class ApkInfoState(
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val fileName: String = "",
    val info: ApkInfo? = null,
    val error: String? = null
)