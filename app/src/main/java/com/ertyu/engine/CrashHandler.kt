package com.ertyu.engine

import android.content.Context
import android.os.Build
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
            } catch (_: Exception) {
                // 忽略保存日志时的二次崩溃
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun handleCrash(context: Context, throwable: Throwable) {
        val timeText = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date())
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stack = sw.toString()
        val summary = throwable.toString()

        // 1) 独立的崩溃文件，放在当前日志目录（跟随用户自定义目录）
        var logPath = ""
        try {
            val dir = AppLogger.getLogDir()
            val file = File(dir, "crash_$stamp.txt")
            file.printWriter().use { w ->
                w.println("=== 崩溃日志 ===")
                w.println("时间: $timeText")
                w.println("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
                w.println("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                w.println("版本: ${versionText(context)}")
                w.println()
                w.println(stack)
            }
            logPath = file.absolutePath
        } catch (_: Exception) { }

        // 2) 混进主日志，方便按时间线排查
        try {
            AppLogger.e("Crash", "应用崩溃：$summary", throwable)
        } catch (_: Exception) { }

        // 3) 标记，下次启动提示用户去看日志
        try {
            CrashMarker.save(context, timeText, summary, logPath)
        } catch (_: Exception) { }
    }

    private fun versionText(context: Context): String = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        "${info.versionName} ($code)"
    } catch (_: Exception) {
        "未知"
    }
}
