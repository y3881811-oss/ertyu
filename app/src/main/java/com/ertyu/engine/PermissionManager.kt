package com.ertyu.engine

/**
 * 修改文件权限（chmod）。
 * 只在 root 通道下有意义；/sdcard 是 FUSE，chmod 基本无效。
 */
object PermissionManager {

    fun isValidMode(mode: String): Boolean =
        mode.matches(Regex("^[0-7]{3,4}$"))

    fun describe(mode: String): String {
        val m = mode.padStart(3, '0').takeLast(3)
        val digits = m.map { it - '0' }
        if (digits.size != 3 || digits.any { it !in 0..7 }) return "无效"
        val rwx = listOf("---", "--x", "-w-", "-wx", "r--", "r-x", "rw-", "rwx")
        return buildString {
            append("文件所有者 ").append(rwx[digits[0]])
            append(" · 组 ").append(rwx[digits[1]])
            append(" · 其他 ").append(rwx[digits[2]])
        }
    }

    private fun quote(path: String): String = "'" + path.replace("'", "'\\''") + "'"

    suspend fun apply(path: String, mode: String, useRoot: Boolean): Result<String> {
        if (!isValidMode(mode)) return Result.failure(IllegalArgumentException("权限值必须是 3~4 位八进制"))
        if (path.isBlank()) return Result.failure(IllegalArgumentException("路径为空"))
        return try {
            val out = ShellExecutor.execute("chmod $mode ${quote(path)}; echo EXIT:\$?", useRoot)
            if (out.contains("Error") || !out.contains("EXIT:0")) {
                Result.failure(IllegalStateException(out.trim().take(300)))
            } else {
                Result.success(out.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 读取当前权限（stat -c %a），需要 root */
    suspend fun readMode(path: String, useRoot: Boolean): String? = try {
        val out = ShellExecutor.execute("stat -c %a ${quote(path)} 2>/dev/null", useRoot).trim()
        out.takeIf { it.matches(Regex("^[0-7]{3,4}$")) }
    } catch (_: Exception) {
        null
    }
}
