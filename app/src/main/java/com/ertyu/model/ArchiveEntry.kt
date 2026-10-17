package com.ertyu.model

data class ArchiveEntry(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val compressedSize: Long,
    val depth: Int
) {
    val sizeText: String
        get() = if (isDirectory) "" else formatSize(size)

    val compressedText: String
        get() = if (isDirectory) "" else formatSize(compressedSize)
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format("%.1f KB", kb)
    val mb = kb / 1024.0
    if (mb < 1024) return String.format("%.1f MB", mb)
    val gb = mb / 1024.0
    return String.format("%.2f GB", gb)
}