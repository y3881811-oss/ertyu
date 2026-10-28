package com.ertyu.engine

import android.os.Environment
import com.ertyu.model.LogFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogManager {

    private val logDir = File(Environment.getExternalStorageDirectory(), "Download/MTFM_Logs")
    private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    /**
     * 抓取当前 App 的运行日志（限制最近 5000 行，防止内存溢出）
     */
    suspend fun saveCurrentLog(): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (!logDir.exists()) logDir.mkdirs()

            val timeStr = dateFormat.format(Date())
            val outFile = File(logDir, "log_$timeStr.txt")

            // 使用 logcat 抓取最近 5000 行，只抓取包含包名的或者全局的
            // -d: 抓取完退出 (dump)
            // -v time: 带时间戳
            // -t 5000: 最近 5000 行
            val process = Runtime.getRuntime().exec("logcat -d -v time -t 5000")
            val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
            
            outFile.bufferedWriter().use { writer ->
                var line: String?
                while (bufferedReader.readLine().also { line = it } != null) {
                    writer.write(line)
                    writer.newLine()
                }
            }
            process.destroy()
            
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 获取已保存的日志文件列表
     */
    suspend fun getLogFiles(): List<LogFile> = withContext(Dispatchers.IO) {
        if (!logDir.exists()) return@withContext emptyList()
        
        logDir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
            ?.sortedByDescending { it.lastModified() }
            ?.map {
                LogFile(
                    name = it.name,
                    path = it.absolutePath,
                    size = formatSize(it.length()),
                    time = displayDateFormat.format(Date(it.lastModified()))
                )
            } ?: emptyList()
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format("%.1f MB", mb)
    }
}