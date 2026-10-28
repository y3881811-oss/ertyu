package com.ertyu.model

enum class ArchiveFormat(val label: String) {
    ZIP("ZIP"),
    TAR("TAR"),
    TAR_GZ("TAR.GZ"),
    TAR_BZ2("TAR.BZ2"),
    TAR_XZ("TAR.XZ"),
    TAR_ZST("TAR.ZST"),
    GZ("GZ"),
    BZ2("BZ2"),
    XZ("XZ"),
    ZST("ZST"),
    SEVEN_Z("7Z"),
    RAR("RAR");

    val canCompress: Boolean
        get() = this == ZIP || this == TAR || this == TAR_GZ || this == SEVEN_Z

    companion object {
        fun detect(fileName: String): ArchiveFormat? {
            val lower = fileName.lowercase()
            return when {
                lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> TAR_GZ
                lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2") || lower.endsWith(".tbz") -> TAR_BZ2
                lower.endsWith(".tar.xz") || lower.endsWith(".txz") -> TAR_XZ
                lower.endsWith(".tar.zst") || lower.endsWith(".tzst") -> TAR_ZST
                lower.endsWith(".tar") -> TAR
                lower.endsWith(".gz") -> GZ
                lower.endsWith(".bz2") -> BZ2
                lower.endsWith(".xz") -> XZ
                lower.endsWith(".zst") -> ZST
                lower.endsWith(".7z") -> SEVEN_Z
                lower.endsWith(".rar") -> RAR
                lower.endsWith(".zip") -> ZIP
                else -> null
            }
        }

        fun isArchive(fileName: String): Boolean = detect(fileName) != null
    }
}