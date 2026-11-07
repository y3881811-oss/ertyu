package com.ertyu.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                handleCrash(context, throwable)
            } catch (e: Exception) {
                // 忽略保存日志时的二次崩溃
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun handleCrash(context: Context, throwable: Throwable) {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
        val fileName = "crash_$time.txt"

        val dir = File(Environment.getExternalStorageDirectory(), "MTFM/logs")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName)

        file.printWriter().use { writer ->
            writer.println("=== 崩溃日志 ===")
            writer.println("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())}")
            writer.println("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
            writer.println("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            
            // 👇 修复：兼容 API 26~27，避免在 Android 8 上崩溃
            try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toString()
                }
                writer.println("版本: ${pInfo.versionName} ($versionCode)")
            } catch (e: Exception) {
                writer.println("版本: 未知")
            }
            writer.println()

            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            writer.println(sw.toString())
        }
    }
}