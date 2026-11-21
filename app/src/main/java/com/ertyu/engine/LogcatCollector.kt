package com.ertyu.engine

import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

object LogcatCollector {
    private var collectJob: Job? = null

    /**
     * 启动后台监听，仅抓取本进程的 Error 和 Warning 级别日志
     */
    fun start() {
        if (collectJob != null) return
        val pid = Process.myPid()
        
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // 参数解释：
                // --pid 过滤仅当前 App
                // *:S 静默所有标签
                // E 和 W 级别输出，不包含 D 和 I 防止文件过大
                val process = Runtime.getRuntime().exec("logcat -v time --pid=$pid *:S E W")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    val logLine = line ?: continue
                    
                    // 过滤掉与 AppLogger 自身相关的日志，避免死循环
                    if (logLine.contains("EYU_Log") || logLine.contains("LogcatCollector")) continue
                    
                    // 提取时间和内容
                    // 原生格式如：11-21 14:30:00.123  1234  1234 E AndroidRuntime: FATAL...
                    val parts = logLine.split(" ", limit = 7)
                    if (parts.size >= 7) {
                        val timeStr = parts[1] // 14:30:00.123
                        val level = parts[4]  // E 或 W
                        val content = parts[6] // 具体报错信息及堆栈第一行
                        
                        AppLogger.sysLog(level, "[$timeStr] $content")
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
    }
}