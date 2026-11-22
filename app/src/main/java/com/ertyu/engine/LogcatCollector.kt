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

object LogcatCollector {
    private const val OWN_TAG = "EYU_Log"
    private var collectJob: Job? = null
    private var process: java.lang.Process? = null

    // threadtime 格式：11-21 20:50:36.123  1234  5678 E AndroidRuntime: 消息
    private val lineRegex = Regex(
        """^\d{2}-\d{2}\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([EWF])\s+(.+?)\s*:\s?(.*)$"""
    )

    /** 与排查无关、只会刷屏的系统噪声 */
    private val NOISE = listOf(
        "jdwp agent",
        "persist.vendor.sys.activitylog",
        "Access denied finding property",
        "Unable to open '/dev/",
        "Failed to read property"
    )

    private data class RunResult(val captured: Int, val exit: Int, val ranMs: Long)

    fun start() {
        if (collectJob != null) return
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            val pid = Process.myPid()
            AppLogger.sysLog("I", "logcat 监听启动 pid=$pid")

            var result = run(listOf("/system/bin/logcat", "-v", "threadtime", "--pid=$pid", "*:W"))
            if (result.captured == 0 && result.ranMs < 5000) {
                // 部分机型（如 vivo）带 --pid 会立刻 EOF，退一步再试一次
                AppLogger.sysLog("W", "logcat 启动即退出(exit=${result.exit})，改用不带 --pid 的方式重试")
                delay(1200)
                result = run(listOf("/system/bin/logcat", "-v", "threadtime", "*:W"))
            }

            if (result.captured == 0) {
                AppLogger.sysLog(
                    "E",
                    "本机无法读取 logcat（exit=${result.exit}），已停止收集。可改用「设置 → 引擎」里的 Shizuku，或在日志页手动抓取快照"
                )
            } else {
                AppLogger.sysLog("W", "logcat 监听结束 exit=${result.exit}，本次共捕获 ${result.captured} 行")
            }
            collectJob = null
        }
    }

    private suspend fun run(cmd: List<String>): RunResult {
        val startedAt = System.currentTimeMillis()
        var captured = 0
        try {
            val proc = Runtime.getRuntime().exec(cmd.toTypedArray())
            process = proc
            val errBuf = StringBuilder()
            val errThread = Thread {
                try {
                    BufferedReader(InputStreamReader(proc.errorStream)).forEachLine { errBuf.appendLine(it) }
                } catch (_: Exception) { }
            }.apply { isDaemon = true; start() }

            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null && collectJob?.isActive != false) {
                    if (handleLine(line)) captured++
                    line = reader.readLine()
                }
            }
            errThread.join(1500)
            val exit = try { proc.waitFor() } catch (_: Exception) { -1 }
            val err = errBuf.toString().trim()
            if (err.isNotEmpty() && captured == 0) {
                AppLogger.sysLog("E", "logcat 错误输出: ${err.take(300)}")
            }
            return RunResult(captured, exit, System.currentTimeMillis() - startedAt)
        } catch (t: Throwable) {
            AppLogger.sysLog("E", "logcat 无法启动: ${t.javaClass.simpleName} ${t.message}")
            return RunResult(captured, -1, System.currentTimeMillis() - startedAt)
        } finally {
            try { process?.destroy() } catch (_: Exception) { }
            process = null
        }
    }

    /** 返回 true 表示这一行被记录 */
    private fun handleLine(line: String): Boolean {
        if (line.contains(OWN_TAG)) return false
        if (NOISE.any { line.contains(it) }) return false

        val m = lineRegex.find(line)
        if (m != null) {
            val timeStr = m.groupValues[1]
            val level = m.groupValues[2]
            val tag = m.groupValues[3].trim()
            val msg = m.groupValues[4].trimEnd()
            AppLogger.sysLog(level, "[$timeStr][$tag] $msg")
            return true
        }

        // 堆栈续行
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
