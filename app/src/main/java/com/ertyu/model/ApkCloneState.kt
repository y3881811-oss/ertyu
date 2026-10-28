package com.ertyu.model

data class ApkCloneState(
    val visible: Boolean = false,
    val apkPath: String = "",
    val apkName: String = "",
    val originalPackage: String = "",
    val newPackage: String = "",
    val isProcessing: Boolean = false,
    val progressMessage: String = "",
    val resultPath: String = "",
    val error: String? = null
) {
    val lengthMatches: Boolean
        get() = newPackage.length == originalPackage.length
}