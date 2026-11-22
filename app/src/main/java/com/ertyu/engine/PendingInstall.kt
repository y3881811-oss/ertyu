package com.ertyu.engine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

/**
 * 记录「已触发安装」，下次启动时判断是否真的装上了。
 * 系统安装器可能被用户取消，这里用于给一次明确提示，而不是让用户以为更新失败。
 */
object PendingInstall {

    private const val PREF = "eyu_pending_install"
    private const val KEY_CODE = "target_code"
    private const val KEY_APK = "apk_path"

    data class Outcome(val success: Boolean, val apkPath: String)

    fun record(context: Context, apkPath: String, targetCode: Long) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putLong(KEY_CODE, targetCode)
                .putString(KEY_APK, apkPath)
                .apply()
        } catch (_: Exception) { }
    }

    /** 读 APK 里的 versionCode（用于和已装版本比较） */
    fun apkVersionCode(context: Context, apk: File): Long {
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0) ?: return 0L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        } catch (_: Exception) {
            0L
        }
    }

    /** 应用启动时调用：有待确认的安装就返回结果并清理记录 */
    fun takeOutcome(context: Context): Outcome? {
        return try {
            val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val target = sp.getLong(KEY_CODE, 0L)
            val apkPath = sp.getString(KEY_APK, "") ?: ""
            if (target <= 0L) return null
            sp.edit().clear().apply()

            val current = try {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
                    @Suppress("DEPRECATION")
                    info.versionCode.toLong()
                }
            } catch (_: PackageManager.NameNotFoundException) {
                0L
            }
            Outcome(success = current >= target, apkPath = apkPath)
        } catch (_: Exception) {
            null
        }
    }
}
