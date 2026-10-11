package com.ertyu.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    sealed class Result {
        object Success : Result()
        object NeedInstallPermission : Result()
        data class Failure(val message: String) : Result()
    }

    /**
     * 通过系统安装器安装 APK。
     * Android 8+ 需要用户先授予本应用 "安装未知应用" 权限。
     */
    fun install(context: Context, apkFile: File): Result {
        if (!apkFile.exists() || !apkFile.isFile) {
            return Result.Failure("APK 文件不存在")
        }
        if (!apkFile.name.lowercase().endsWith(".apk")) {
            return Result.Failure("不是 APK 文件")
        }

        // Android 8+ 检查"安装未知应用"权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                return Result.NeedInstallPermission
            }
        }

        return try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
            Result.Success
        } catch (e: Exception) {
            Result.Failure(e.message ?: "启动安装器失败")
        }
    }

    /**
     * 打开系统的"安装未知应用"设置页，让用户给本应用授权。
     * 用户返回后需要再次点击安装。
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