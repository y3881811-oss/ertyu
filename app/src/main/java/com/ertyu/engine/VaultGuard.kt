package com.ertyu.engine

/** 判断某路径是否属于隐私空间，用于搜索/缩略图等全局扫描时跳过 */
object VaultGuard {
    private const val NAME = ".EYU_Vault"

    fun isVaultPath(path: String): Boolean {
        if (path.isBlank()) return false
        return path.contains("/$NAME/") || path.endsWith("/$NAME")
    }

    fun isVaultDirName(name: String): Boolean = name == NAME
}
