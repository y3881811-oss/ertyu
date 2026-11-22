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

/**
 * 采集本应用自己的 logcat。
 *
 * 优先流式（logcat -v threadtime --pid=<pid> *:W）；
 * 部分 ROM 会立刻 EOF，此时自动降级为「每 8 秒一次 logcat -d 快照」。
 * 两种方式都只保留本进程 pid 的行，绝不把整机日志写进文件。
 */
object LogcatCollector {
    private const val OWN_TAG = "EYU_Log"
    private const val DUMP_INTERVAL_MS = 8000L
    private const val MAX_SEEN = 3000

    private var collectJob: Job? = null
    private var process: java.lang.Process? = null
    private val seenLines = LinkedHashSet<String>()

    private val lineRegex = Regex(
        """^\d{2}-\d{2}\s+(\d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+\d+\s+([EWF])\s+(.+?)\s*:\s?(.*)$"""
    )

    private val NOISE = listOf(
        "jdwp agent",
        "persist.vendor.sys.activitylog",
        "Access denied finding property",
        "Unable to open '/dev/",
        "Failed to read property",
        "initVivoBgFallback",
        "BackgroundFallback",
        "ioctl c0044901"
    )

    private data class RunResult(val captured: Int, val exit: Int, val ranMs: Long)

    fun start() {
        if (collectJob != null) return
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            val pid = Process.myPid()
            AppLogger.sysLog("I", "logcat 监听启动 pid=$pid")

            val streaming = stream(listOf("/system/bin/logcat", "-v", "threadtime", "--pid=$pid", "*:W"), pid)
            AppLogger.sysLog(
                "I",
                "logcat 流式结果：捕获 ${streaming.captured} 行，exit=${streaming.exit}，持续 ${streaming.ranMs}ms"
            )

            if (streaming.captured == 0 && streaming.ranMs < 5000) {
                AppLogger.sysLog("W", "流式不可用，切换为 -d 轮询快照（每 ${DUMP_INTERVAL_MS / 1000}s 一次）")
                var emptyRounds = 0
                var total = 0
                while (collectJob?.isActive == true && emptyRounds < 3) {
                    delay(DUMP_INTERVAL_MS)
                    val n = dumpOnce(pid)
                    total += n
                    emptyRounds = if (n == 0) emptyRounds + 1 else 0
                }
                if (emptyRounds >= 3) {
                    AppLogger.sysLog(
                        "E",
                        "本机禁止读取 logcat：流式与 -d 都没有本应用输出（可能是 vivo 的系统策略）。" +
                            "需要系统日志时请用 Shizuku 引擎或 ADB"
                    )
                } else {
                    AppLogger.sysLog("W", "logcat -d 轮询结束，本次共捕获 $total 行")
                }
            } else {
                AppLogger.sysLog("W", "logcat 监听结束 exit=${streaming.exit}，捕获 ${streaming.captured} 行")
            }
            collectJob = null
        }
    }

    private suspend fun stream(cmd: List<String>, selfPid: Int): RunResult {
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
                    if (handleLine(line, selfPid)) captured++
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

    /** 一次性快照：logcat -d，返回新增记录行数 */
    private suspend fun dumpOnce(selfPid: Int): Int {
        var captured = 0
        try {
            val proc = Runtime.getRuntime().exec(
                arrayOf("/system/bin/logcat", "-d", "-v", "threadtime", "--pid=$selfPid", "*:W")
            )
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (handleLine(line, selfPid)) captured++
                    line = reader.readLine()
                }
            }
            try { proc.waitFor() } catch (_: Exception) { }
        } catch (t: Throwable) {
            AppLogger.sysLog("E", "logcat -d 失败: ${t.javaClass.simpleName} ${t.message}")
        }
        return captured
    }

    /** 返回 true 表示这一行被记录（只记录本应用 pid 的行，并按内容去重） */
    private fun handleLine(line: String, selfPid: Int): Boolean {
        if (line.contains(OWN_TAG)) return false
        if (NOISE.any { line.contains(it) }) return false

        val m = lineRegex.find(line)
        if (m != null) {
            val pid = m.groupValues[2].toIntOrNull() ?: return false
            if (pid != selfPid) return false
            val timeStr = m.groupValues[1]
            val level = m.groupValues[3]
            val tag = m.groupValues[4].trim()
            val msg = m.groupValues[5].trimEnd()
            val key = "$timeStr|$level|$tag|$msg"
            if (!rememberLine(key)) return false
            AppLogger.sysLog(level, "[$timeStr][$tag] $msg")
            return true
        }

        val t = line.trimStart()
        if (t.startsWith("at ") || t.startsWith("Caused by:") ||
            t.startsWith("Suppressed:") || t.startsWith("... ")
        ) {
            if (!rememberLine("raw|$line")) return false
            AppLogger.sysLogRaw(line.trimEnd())
            return true
        }
        return false
    }

    /** -d 轮询会重复吐出同样的行，这里做去重 */
    private fun rememberLine(key: String): Boolean {
        if (seenLines.size > MAX_SEEN) seenLines.clear()
        return seenLines.add(key)
    }

    fun stop() {
        try { process?.destroy() } catch (_: Exception) { }
        process = null
        collectJob?.cancel()
        collectJob = null
    }
}
