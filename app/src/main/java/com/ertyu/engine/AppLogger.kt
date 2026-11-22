package com.ertyu.engine

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val TAG = "EYU_Log"
    private const val PREFS_NAME = "EYU_Log_Prefs"
    private const val KEY_LOG_DIR = "custom_log_dir"
    private const val MAX_FILE_BYTES = 5L * 1024 * 1024

    var isVerboseLoggingEnabled = true
    private var customLogPath: String? = null

    // 单线程 + 锁：保证中文行与 logcat 行（含堆栈）写入顺序不乱
    private val ioScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    private val writeLock = Mutex()

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
        val custom = customLogPath
        if (!custom.isNullOrBlank()) {
            val f = File(custom)
            if (f.exists() || f.mkdirs()) return f
        }
        val defaultDir = File(Environment.getExternalStorageDirectory(), "EYU文件管理器/logs")
        if (!defaultDir.exists()) defaultDir.mkdirs()
        return defaultDir
    }

    fun d(module: String, message: String) {
        if (!isVerboseLoggingEnabled) return
        Log.d(TAG, "[$module] $message")
        writeLine("D", module, message)
    }

    fun i(module: String, message: String) {
        Log.i(TAG, "[$module] $message")
        writeLine("I", module, message)
    }

    fun w(module: String, message: String) {
        Log.w(TAG, "[$module] $message")
        writeLine("W", module, message)
    }

    fun e(module: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$module] $message", throwable)
        val errorMsg = if (throwable != null) "$message\n${Log.getStackTraceString(throwable)}" else message
        writeLine("E", module, errorMsg)
    }

    /** 供 LogcatCollector 使用：带级别写入一行 */
    fun sysLog(level: String, message: String) {
        writeLine(level, "SYS", message)
    }

    /** 供 LogcatCollector 使用：原样写入一行（堆栈续行，不加时间戳） */
    fun sysLogRaw(text: String) {
        if (text.isBlank()) return
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        ioScope.launch {
            writeLock.withLock { appendRaw(dateStr, "$text\n") }
        }
    }

    private fun writeLine(level: String, module: String, message: String) {
        val now = Date()
        val timeStr = SimpleDateFormat("HH:mm:ss.SSS", Locale.ROOT).format(now)
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now)
        ioScope.launch {
            writeLock.withLock { appendRaw(dateStr, "$timeStr [$level] [$module] $message\n") }
        }
    }

    private fun appendRaw(dateStr: String, text: String) {
        try {
            val logFile = File(getLogDir(), "app_$dateStr.log")
            if (logFile.exists() && logFile.length() > MAX_FILE_BYTES) return
            logFile.appendText(text)
        } catch (_: Exception) { }
    }
}