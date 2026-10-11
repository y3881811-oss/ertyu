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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                return@withContext Result.NeedInstallPermission
            }
        }

        try {
            val installer = context.packageManager.packageInstaller

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