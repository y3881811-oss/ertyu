package com.ertyu.engine

import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.atomic.AtomicInteger

object LogcatCollector {
    private const val OWN_TAG = "EYU_Log"
    private var collectJob: Job? = null
    private var process: java.lang.Process? = null

    // threadtime 格式：11-21 20:50:36.123  1234  5678 E AndroidRuntime: 消息
    private val lineRegex = Regex(
        """^\d{2}-\d{2}\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([EWF])\s+(.+?)\s*:\s?(.*)$"""
    )

    fun start() {
        if (collectJob != null) return
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            val pid = Process.myPid()
            val captured = AtomicInteger(0)
            try {
                // *:W = W 及以上（含 E、F），不是 "标签 E / 标签 W"
                process = Runtime.getRuntime().exec(
                    arrayOf("/system/bin/logcat", "-v", "threadtime", "--pid=$pid", "*:W")
                )
                val proc = process!!

                // 收集 stderr，出错时写进日志，方便定位
                val errBuf = StringBuilder()
                Thread {
                    try {
                        BufferedReader(InputStreamReader(proc.errorStream))
                            .forEachLine { errBuf.appendLine(it) }
                    } catch (_: Exception) { }
                }.apply { isDaemon = true; start() }

                AppLogger.sysLog("I", "logcat 监听启动 pid=$pid")

                // 启动 5 秒后仍 0 行，写一条提示，便于判断是否被系统限制
                val watcher = launch {
                    delay(5000)
                    if (captured.get() == 0) {
                        AppLogger.sysLog("W", "5 秒内未捕获任何 W/E 级 logcat（安静或读取受限）")
                    }
                }

                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null && isActive) {
                        if (handleLine(line)) captured.incrementAndGet()
                        line = reader.readLine()
                    }
                }
                watcher.cancel()

                val code = try { proc.waitFor() } catch (_: Exception) { -1 }
                if (errBuf.isNotBlank()) {
                    AppLogger.sysLog("E", "logcat 错误输出: ${errBuf.toString().trim()}")
                }
                AppLogger.sysLog("W", "logcat 监听结束 exit=$code")
            } catch (t: Throwable) {
                AppLogger.sysLog("E", "logcat 无法启动: ${t.javaClass.simpleName} ${t.message}")
            } finally {
                try { process?.destroy() } catch (_: Exception) { }
                process = null
                collectJob = null
            }
        }
    }

    /** 返回 true 表示这一行是有效的结构化日志 */
    private fun handleLine(line: String): Boolean {
        // 跳过 AppLogger 自己发出的中文日志（已经是 EYU_Log 标签，避免重复）
        if (line.contains(OWN_TAG)) return false

        val m = lineRegex.find(line)
        if (m != null) {
            val timeStr = m.groupValues[1]
            val level = m.groupValues[2]
            val tag = m.groupValues[3].trim()
            val msg = m.groupValues[4].trimEnd()
            AppLogger.sysLog(level, "[$timeStr][$tag] $msg")
            return true
        }

        // 堆栈续行（at ... / Caused by: / Suppressed: / ... 23 more）原样保留
        val t = line.trimStart()
        if (t.startsWith("at ") || t.startsWith("Caused by:") ||
            t.startsWith("Suppressed:") || t.startsWith("... ")
        ) {
            AppLogger.sysLogRaw(line.trimEnd())
            return true
        }
        return false
    }

    fun stop() {
        try { process?.destroy() } catch (_: Exception) { }
        process = null
        collectJob?.cancel()
        collectJob = null
    }
}