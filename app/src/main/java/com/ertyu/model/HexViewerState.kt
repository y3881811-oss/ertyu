package com.ertyu.model

data class HexViewerState(
    val visible: Boolean = false,
    val filePath: String = "",
    val fileName: String = "",
    val fileSize: Long = 0,
    val loadedBytes: Int = 0,
    val bytes: ByteArray = ByteArray(0),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HexViewerState) return false
        return visible == other.visible &&
                filePath == other.filePath &&
                loadedBytes == other.loadedBytes &&
                isLoading == other.isLoading
    }

    override fun hashCode(): Int {
        var result = visible.hashCode()
        result = 31 * result + filePath.hashCode()
        result = 31 * result + loadedBytes
        result = 31 * result + isLoading.hashCode()
        return result
    }
}