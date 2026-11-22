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
 * 优先流式（logcat -v threadtime --pid=<pid> *:W）。
 * 部分 ROM 的流式通道会立刻 EOF，此时降级为「每 8 秒一次快照」。
 *
 * 判定规则很重要：能不能读到输出，看的是「快照有没有读到任何一行」，
 * 而不是「有没有本应用的行」——后者只能说明应用这段时间没打 W/E 日志。
 */
object LogcatCollector {
    private const val OWN_TAG = "EYU_Log"
    private const val DUMP_INTERVAL_MS = 8000L
    private const val MAX_SEEN = 3000
    private const val TAIL_LINES = 400

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

    private data class StreamResult(val captured: Int, val exit: Int, val ranMs: Long)
    private data class DumpResult(val total: Int, val own: Int, val exit: Int, val usedTail: Boolean)

    fun start() {
        if (collectJob != null) return
        collectJob = CoroutineScope(Dispatchers.IO).launch {
            val pid = Process.myPid()
            AppLogger.sysLog("I", "logcat 监听启动 pid=$pid")

            val streaming = stream(listOf("/system/bin/logcat", "-v", "threadtime", "--pid=$pid", "*:W"), pid)
            AppLogger.sysLog(
                "I",
                "logcat 流式结果：本应用 ${streaming.captured} 行，exit=${streaming.exit}，持续 ${streaming.ranMs}ms"
            )

            if (streaming.captured > 0 || streaming.ranMs >= 5000) {
                AppLogger.sysLog("W", "logcat 监听结束 exit=${streaming.exit}，本应用 ${streaming.captured} 行")
                collectJob = null
                return@launch
            }

            // ---- 流式不可用，改快照轮询 ----
            val first = dumpOnce(pid)
            if (first.total == 0) {
                AppLogger.sysLog(
                    "E",
                    "本机禁止读取 logcat：快照也读不到任何输出（exit=${first.exit}）。需要系统日志请用 Shizuku 引擎或 ADB"
                )
                collectJob = null
                return@launch
            }

            AppLogger.sysLog(
                "I",
                "快照模式可用（本次读到 ${first.total} 行，其中本应用 ${first.own} 行，tail=${first.usedTail}），开始每 ${DUMP_INTERVAL_MS / 1000}s 轮询"
            )
            var totalOwn = first.own
            while (collectJob?.isActive == true) {
                delay(DUMP_INTERVAL_MS)
                val r = dumpOnce(pid)
                totalOwn += r.own
            }
            AppLogger.sysLog("W", "logcat 快照轮询结束，本次共捕获本应用 $totalOwn 行")
            collectJob = null
        }
    }

    private suspend fun stream(cmd: List<String>, selfPid: Int): StreamResult {
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
            return StreamResult(captured, exit, System.currentTimeMillis() - startedAt)
        } catch (t: Throwable) {
            AppLogger.sysLog("E", "logcat 无法启动: ${t.javaClass.simpleName} ${t.message}")
            return StreamResult(captured, -1, System.currentTimeMillis() - startedAt)
        } finally {
            try { process?.destroy() } catch (_: Exception) { }
            process = null
        }
    }

    /**
     * 一次快照。先用 -t 取尾部（避免整缓冲），命令报错再退回纯 -d。
     * 关键：能否读到日志看的是「命令正常执行 / 读到行」，而不是「有没有本应用的行」。
     */
    private suspend fun dumpOnce(selfPid: Int): DumpResult {
        val tail = runDump(
            arrayOf("/system/bin/logcat", "-v", "threadtime", "-t", TAIL_LINES.toString(), "*:W"),
            selfPid
        )
        // exit == 0 表示命令执行成功（哪怕这段时间没有任何 W/E 日志）
        if (tail.first > 0 || tail.third == 0) {
            return DumpResult(tail.first, tail.second, tail.third, true)
        }
        val full = runDump(arrayOf("/system/bin/logcat", "-d", "-v", "threadtime", "*:W"), selfPid)
        return DumpResult(full.first, full.second, full.third, false)
    }

    private suspend fun runDump(cmd: Array<String>, selfPid: Int): Triple<Int, Int, Int> {
        var total = 0
        var own = 0
        var exit = -1
        try {
            val proc = Runtime.getRuntime().exec(cmd)
            BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    total++
                    if (handleLine(line, selfPid)) own++
                    line = reader.readLine()
                }
            }
            exit = try { proc.waitFor() } catch (_: Exception) { -1 }
        } catch (t: Throwable) {
            AppLogger.sysLog("E", "logcat 快照失败: ${t.javaClass.simpleName} ${t.message}")
        }
        return Triple(total, own, exit)
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
