package com.ertyu.engine

import android.content.Context
import java.io.File

/** 崩溃标记：崩溃时写一份摘要，下次启动弹出提示，用户可一键去看日志 */
object CrashMarker {

    private const val FILE = "last_crash.txt"

    data class Info(val timeText: String, val summary: String, val logPath: String)

    fun save(context: Context, timeText: String, summary: String, logPath: String) {
        try {
            File(context.filesDir, FILE).writeText(
                listOf(timeText, summary.take(300), logPath).joinToString("\n")
            )
        } catch (_: Exception) { }
    }

    fun read(context: Context): Info? {
        return try {
            val f = File(context.filesDir, FILE)
            if (!f.isFile) return null
            val lines = f.readLines()
            if (lines.size < 3) return null
            Info(lines[0], lines[1], lines[2])
        } catch (_: Exception) {
            null
        }
    }

    fun clear(context: Context) {
        try { File(context.filesDir, FILE).delete() } catch (_: Exception) { }
    }
}
