package com.ertyu.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "EYU_Log"
    private const val PREFS_NAME = "EYU_Log_Prefs"
    private const val KEY_LOG_DIR = "custom_log_dir"

    var isVerboseLoggingEnabled = true 
    private var customLogPath: String? = null

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        customLogPath = prefs.getString(KEY_LOG_DIR, null)
    }

    fun setCustomPath(context: Context, path: String?) {
        customLogPath = path
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LOG_DIR, path).apply()
    }

    fun getCustomPath(): String? = customLogPath

    fun getLogDir(): File {
        if (!customLogPath.isNullOrBlank()) {
            val f = File(customLogPath!!)
            if (f.exists() || f.mkdirs()) return f
        }
        val defaultDir = File(Environment.getExternalStorageDirectory(), "EYU文件管理器/logs")
        if (!defaultDir.exists()) defaultDir.mkdirs()
        return defaultDir
    }

    fun d(module: String, message: String) {
        if (!isVerboseLoggingEnabled) return
        Log.d(TAG, "[$module] $message")
        writeToFile("D", module, message)
    }

    fun i(module: String, message: String) {
        Log.i(TAG, "[$module] $message")
        writeToFile("I", module, message)
    }

    fun w(module: String, message: String) {
        Log.w(TAG, "[$module] $message")
        writeToFile("W", module, message)
    }

    fun e(module: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$module] $message", throwable)
        val errorMsg = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        writeToFile("E", module, errorMsg)
    }

    // 👇 新增：专供系统 Logcat 收集器调用
    fun sysLog(level: String, message: String) {
        writeToFile(level, "SYS", message)
    }

    private fun writeToFile(level: String, module: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dir = getLogDir()
                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                val logFile = File(dir, "app_$dateStr.log")
                val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(Date())
                
                // 如果是 SYS 模块，说明已经是带时间戳的原生日志，直接记录
                val logLine = if (module == "SYS") {
                    "$timeStr [SYS] $message\n"
                } else {
                    "$timeStr [$level] [$module] $message\n"
                }
                
                // 限制文件大小，防止撑爆手机（例如单文件大于 5MB 就不再写入）
                if (logFile.exists() && logFile.length() > 5 * 1024 * 1024) return@launch
                
                logFile.appendText(logLine)
            } catch (_: Exception) { }
        }
    }
}