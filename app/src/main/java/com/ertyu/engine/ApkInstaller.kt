package com.ertyu.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

object ApkInstaller {

    const val ACTION_INSTALL_RESULT = "com.ertyu.INSTALL_RESULT"

    private const val BUFFER_SIZE = 64 * 1024

    sealed class Result {
        object Success : Result()
        object NeedInstallPermission : Result()
        data class Failure(val message: String) : Result()
    }

    /**
     * 用 PackageInstaller Session API 安装 APK。
     * 优势：跳过 FileProvider URI 传递，直接流式写入系统 session，
     * 减少一次跨进程 IPC + URI 解析，大文件提速明显。
     */
    suspend fun install(
        context: Context,
        apkFile: File,
        onProgress: (copied: Long, total: Long) -> Unit = { _, _ -> }
    ): Result = withContext(Dispatchers.IO) {
        if (!apkFile.exists() || !apkFile.isFile) {
            return@withContext Result.Failure("APK 文件不存在")
        }
        if (!apkFile.name.lowercase().endsWith(".apk")) {
            return@withContext Result.Failure("不是 APK 文件")
        }

        // Android 8+ 需要"安装未知应用"权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                return@withContext Result.NeedInstallPermission
            }
        }

        try {
            val installer = context.packageManager.packageInstaller

            // 预读包名，加速系统分配 & 覆盖判断
            val packageName = runCatching {
                context.packageManager
                    .getPackageArchiveInfo(
                        apkFile.absolutePath,
                        PackageManager.GET_META_DATA
                    )
                    ?.packageName
            }.getOrNull()

            val params = PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL
            ).apply {
                if (!packageName.isNullOrBlank()) {
                    setAppPackageName(packageName)
                }
                // 允许系统智能选择安装位置（内部/外部存储）
                setInstallLocation(PackageInstaller.SessionParams.INSTALL_LOCATION_AUTO)
            }

            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            try {
                val total = apkFile.length()
                onProgress(0, total)

                session.openWrite("base.apk", 0, total).use { out ->
                    FileInputStream(apkFile).use { input ->
                        val buf = ByteArray(BUFFER_SIZE)
                        var copied = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            copied += n
                            onProgress(copied, total)
                        }
                        session.fsync(out)
                    }
                }

                // 结果接收器（Android 9 上 getBroadcast 无需 FLAG_MUTABLE）
                val resultIntent = Intent(ACTION_INSTALL_RESULT)
                    .setPackage(context.packageName)
                val flags = PendingIntent.FLAG_UPDATE_CURRENT
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    resultIntent,
                    flags
                )

                session.commit(pendingIntent.intentSender)
            } finally {
                session.close()
            }

            Result.Success
        } catch (e: Exception) {
            Result.Failure(e.message ?: "安装失败")
        }
    }

    /**
     * 用户从设置返回后，UI 会再让用户点一次安装。
     */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
        }
    }
}