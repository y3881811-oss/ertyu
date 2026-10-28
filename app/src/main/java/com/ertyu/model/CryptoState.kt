package com.ertyu.model

enum class CryptoMode {
    ENCRYPT, DECRYPT
}

data class CryptoState(
    val visible: Boolean = false,
    val mode: CryptoMode = CryptoMode.ENCRYPT,
    val sourceIsLeft: Boolean = true,
    val filePath: String = "",
    val fileName: String = "",
    val isProcessing: Boolean = false,
    val error: String? = null
)