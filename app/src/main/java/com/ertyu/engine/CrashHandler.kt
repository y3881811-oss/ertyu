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

    private var context: Context? = null
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    fun init(ctx: Context) {
        context = ctx.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveCrashLog(thread, throwable)
            } catch (e: Exception) {
                // 防止保存日志时再次发生异常
            } finally {
                // 交还给系统默认处理（弹出系统崩溃提示）
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun saveCrashLog(thread: Thread, throwable: Throwable) {
        val ctx = context ?: return
        val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "crash_$timeStr.txt"

        val logDir = File(Environment.getExternalStorageDirectory(), "Download/MTFM_Logs")
        if (!logDir.exists()) logDir.mkdirs()

        val crashFile = File(logDir, fileName)
        
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        
        val crashInfo = buildString {
            appendLine("========== MTFM 崩溃日志 ==========")
            appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
            appendLine("线程: ${thread.name}")
            appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            
            // 尝试获取 App 版本
            try {
                val pInfo = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
                appendLine("版本: ${pInfo.versionName} (${pInfo.longVersionCode})")
            } catch (e: Exception) {
                // ignore
            }
            
            appendLine("==================================")
            appendLine()
            append(stringWriter.toString())
        }

        crashFile.writeText(crashInfo)
        printWriter.close()
        stringWriter.close()
    }
}