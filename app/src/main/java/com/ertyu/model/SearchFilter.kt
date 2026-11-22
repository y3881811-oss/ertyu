package com.ertyu.model

import java.io.File

enum class FileKindFilter(val label: String) {
    ALL("全部"),
    IMAGE("图片"),
    VIDEO("视频"),
    AUDIO("音频"),
    DOC("文档"),
    ARCHIVE("压缩包"),
    APK("安装包")
}

/** 搜索过滤条件：类型 / 最小大小 / 修改时间 */
data class SearchFilter(
    val kind: FileKindFilter = FileKindFilter.ALL,
    val minSizeMb: Int = 0,
    val modifiedWithinDays: Int = 0
) {
    val isDefault: Boolean
        get() = kind == FileKindFilter.ALL && minSizeMb == 0 && modifiedWithinDays == 0

    fun matches(file: File): Boolean {
        if (file.isDirectory) {
            // 目录：只在"全部 + 不限大小 + 不限时间"时保留，避免一堆空目录
            return kind == FileKindFilter.ALL && minSizeMb == 0 && modifiedWithinDays == 0
        }
        val ext = file.extension.lowercase()
        val kindOk = when (kind) {
            FileKindFilter.ALL -> true
            FileKindFilter.IMAGE -> ext in IMAGE_EXTS
            FileKindFilter.VIDEO -> ext in VIDEO_EXTS
            FileKindFilter.AUDIO -> ext in AUDIO_EXTS
            FileKindFilter.DOC -> ext in DOC_EXTS
            FileKindFilter.ARCHIVE -> ext in ARCHIVE_EXTS
            FileKindFilter.APK -> ext == "apk"
        }
        if (!kindOk) return false

        if (minSizeMb > 0 && file.length() < minSizeMb.toLong() * 1024L * 1024L) return false

        if (modifiedWithinDays > 0) {
            val cutoff = System.currentTimeMillis() - modifiedWithinDays.toLong() * 24L * 3600L * 1000L
            if (file.lastModified() < cutoff) return false
        }
        return true
    }

    companion object {
        private val IMAGE_EXTS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg")
        private val VIDEO_EXTS = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v", "ts")
        private val AUDIO_EXTS = setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma", "opus")
        private val DOC_EXTS = setOf("txt", "md", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "json", "xml", "html", "csv", "log")
        private val ARCHIVE_EXTS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "zst", "apks", "xapk", "apkm")
    }
}
