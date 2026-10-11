package com.ertyu.model

import android.graphics.Bitmap

data class SignerCertificate(
    val subject: String,
    val issuer: String,
    val serialNumber: String,
    val validFrom: String,
    val validTo: String,
    val md5: String,
    val sha1: String,
    val sha256: String
)

data class ApkSignature(
    val signers: List<SignerCertificate> = emptyList(),
    val v1: Boolean = false,
    val v2: Boolean = false,
    val v3: Boolean = false,
    val v31: Boolean = false
) {
    val schemeText: String
        get() = buildList {
            if (v1) add("V1")
            if (v2) add("V2")
            if (v3) add("V3")
            if (v31) add("V3.1")
        }.joinToString(" + ").ifEmpty { "无签名" }
}

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
    val icon: Bitmap?,
    val signature: ApkSignature?
)

data class ApkInfoState(
    val visible: Boolean = false,
    val isLoading: Boolean = false,
    val fileName: String = "",
    val info: ApkInfo? = null,
    val error: String? = null
)