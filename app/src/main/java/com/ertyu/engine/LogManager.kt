package com.ertyu.engine

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ertyu.model.LogFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {

    /**
     * 获取当前日志目录下的所有日志文件（按修改时间倒序）
     */
    fun getLogFiles(): List<LogFile> {
        val logDir = AppLogger.getLogDir()
        if (!logDir.exists() || !logDir.isDirectory) return emptyList()

        val files = logDir.listFiles { file -> file.isFile && file.name.endsWith(".log") } ?: return emptyList()

        return files.sortedByDescending { it.lastModified() }.map { file ->
            val sizeStr = formatSize(file.length())
            val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).format(Date(file.lastModified()))
            LogFile(
                name = file.name,
                path = file.absolutePath,
                size = sizeStr,
                time = timeStr
            )
        }
    }

    /**
     * 分享指定的日志文件
     */
    fun shareLogFile(context: Context, logFile: LogFile) {
        try {
            val file = File(logFile.path)
            if (!file.exists()) return

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "分享日志").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            // 忽略分享异常
        }
    }

    /**
     * 删除指定的日志文件
     */
    fun deleteLogFile(logFile: LogFile): Boolean {
        return try {
            val file = File(logFile.path)
            if (file.exists()) file.delete() else false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 格式化文件大小
     */
    private fun formatSize(size: Long): String {
        if (size < 1024) return "$size B"
        val kb = size / 1024.0
        if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.ROOT, "%.1f MB", mb)
    }
}