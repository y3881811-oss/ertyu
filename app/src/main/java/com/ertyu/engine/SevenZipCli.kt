package com.ertyu.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 加密 7z 写入需要外部 7zz 二进制（commons-compress 不支持）。
 * 约定：把 arm64 的 7zz 放进 jniLibs 并命名为 lib7zz.so，
 * 运行时从 nativeLibraryDir 执行（Android 10+ 只允许从 lib 目录 exec）。
 */
object SevenZipCli {
    private const val BIN_NAME = "lib7zz.so"

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun binFile(): File? {
        val ctx = appContext ?: return null
        return try {
            File(ctx.applicationInfo.nativeLibraryDir, BIN_NAME)
        } catch (_: Exception) {
            null
        }
    }

    fun isAvailable(): Boolean = binFile()?.isFile == true

    suspend fun compress(
        sources: List<File>,
        target: File,
        password: String,
        level: Int = 5
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val bin = binFile()
        if (bin == null || !bin.isFile) {
            return@withContext Result.failure(
                IllegalStateException("当前安装包未内置 7zz 组件，无法写加密 7z；可改用 ZIP（AES-256）")
            )
        }
        if (sources.isEmpty()) return@withContext Result.failure(IllegalArgumentException("没有要压缩的文件"))

        try {
            // 以所有源的共同父目录为工作目录，保证包内路径是相对路径
            val cwd = sources.mapNotNull { it.parentFile }.distinct().firstOrNull() ?: target.parentFile
                ?: return@withContext Result.failure(IllegalArgumentException("无法确定工作目录"))
            val rel = sources.map { it.absolutePath.removePrefix(cwd.absolutePath).trimStart('/') }
            val cmd = ArrayList<String>()
            cmd.add(bin.absolutePath)
            cmd.add("a")
            cmd.add("-t7z")
            cmd.add("-mx=$level")
            cmd.add("-p$password")
            cmd.add("-y")
            cmd.add(target.absolutePath)
            cmd.addAll(rel)

            val proc = try {
                val pb = ProcessBuilder(cmd).directory(cwd).redirectErrorStream(true)
                // 7zz 依赖 libc++_shared.so，两者都在 nativeLibraryDir
                val libDir = bin.parentFile?.absolutePath.orEmpty()
                if (libDir.isNotEmpty()) {
                    val env = pb.environment()
                    val old = env["LD_LIBRARY_PATH"]
                    env["LD_LIBRARY_PATH"] = if (old.isNullOrBlank()) libDir else "$libDir:$old"
                }
                pb.start()
            } catch (e: Exception) {
                return@withContext Result.failure(IllegalStateException("7zz 无法启动：${e.message}"))
            }
            val output = proc.inputStream.bufferedReader().readText()
            val code = proc.waitFor()
            if (code != 0 || !target.isFile) {
                return@withContext Result.failure(
                    IllegalStateException("7zz 失败（code=$code）：${output.take(200)}")
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
