package com.ertyu.model

data class ApkSignerState(
    val visible: Boolean = false,
    val apkPath: String = "",
    val apkName: String = "",
    val keystorePath: String = "",
    val keystoreName: String = "",
    val storePass: String = "",
    val keyAlias: String = "key0",
    val keyPass: String = "",
    val isSigning: Boolean = false,
    val progressMessage: String = "",
    val resultPath: String = "",
    val error: String? = null
)