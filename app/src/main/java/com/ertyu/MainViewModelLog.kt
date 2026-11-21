package com.ertyu

import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.model.LogEntry
import com.ertyu.model.LogState
import com.ertyu.model.LogStateHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

fun MainViewModel.openLogViewer() {
    LogStateHolder.state.value = LogStateHolder.state.value.copy(visible = true)
    refreshLogs()
}

fun MainViewModel.closeLogViewer() {
    LogStateHolder.state.value = LogState(visible = false)
}

fun MainViewModel.updateLogSearch(query: String) {
    LogStateHolder.state.update { it.copy(searchQuery = query) }
}

fun MainViewModel.setLogFilter(level: String?) {
    LogStateHolder.state.update { it.copy(filterLevel = level) }
}

fun MainViewModel.updateLogDirectory(newPath: String?) {
    AppLogger.setCustomPath(getApplication(), newPath)
    showToast("日志目录已更新: ${AppLogger.getLogDir().absolutePath}")
    refreshLogs()
}

fun MainViewModel.refreshLogs() {
    if (LogStateHolder.state.value.isLoading) return
    LogStateHolder.state.update { it.copy(isLoading = true) }

    viewModelScope.launch(Dispatchers.IO) {
        // 1. 读取结构化应用日志
        val logDir = AppLogger.getLogDir()
        val allEntries = mutableListOf<LogEntry>()
        if (logDir.exists() && logDir.isDirectory) {
            logDir.listFiles()?.sortedByDescending { it.lastModified() }?.take(7)?.forEach { file ->
                try {
                    file.forEachLine { line ->
                        val regex = Regex("^(\\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[(\\w)\\] \\[(.*?)\\] (.*)$")
                        val match = regex.find(line)
                        if (match != null) {
                            allEntries.add(LogEntry(match.groupValues[1], match.groupValues[2], match.groupValues[3], match.groupValues[4]))
                        } else if (line.isNotBlank() && allEntries.isNotEmpty()) {
                            val last = allEntries.removeAt(allEntries.size - 1)
                            allEntries.add(last.copy(message = last.message + "\n" + line))
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        // 2. 抓取底层系统 Logcat
        val logcatLines = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("logcat -d -v time")
            val reader = process.inputStream.bufferedReader()
            var line: String?
            // 只取最近 1500 行，防止卡顿
            while (reader.readLine().also { line = it } != null) {
                logcatLines.add(line!!)
                if (logcatLines.size > 1500) logcatLines.removeAt(0)
            }
            reader.close()
        } catch (_: Exception) { }

        withContext(Dispatchers.Main) {
            LogStateHolder.state.update { 
                it.copy(
                    logs = allEntries.reversed(), 
                    logcatLines = logcatLines,
                    isLoading = false
                ) 
            }
        }
    }
}

fun MainViewModel.clearLogs() {
    viewModelScope.launch(Dispatchers.IO) {
        AppLogger.getLogDir().listFiles()?.forEach { it.delete() }
        withContext(Dispatchers.Main) {
            LogStateHolder.state.update { it.copy(logs = emptyList(), logcatLines = emptyList()) }
        }
    }
}

fun MainViewModel.exportLogs() {
    viewModelScope.launch(Dispatchers.IO) {
        LogStateHolder.state.update { it.copy(isExporting = true) }
        try {
            val logDir = AppLogger.getLogDir()
            val state = LogStateHolder.state.value
            
            if ((!logDir.exists() || logDir.listFiles().isNullOrEmpty()) && state.logcatLines.isEmpty()) {
                withContext(Dispatchers.Main) {
                    showToast("没有日志可导出")
                    LogStateHolder.state.update { it.copy(isExporting = false) }
                }
                return@launch
            }

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
            val zipFile = File(android.os.Environment.getExternalStorageDirectory(), "EYU_Logs_$dateStr.zip")
            
            ZipOutputStream(zipFile.outputStream().buffered()).use { zos ->
                // 打包 .log 文件
                logDir.listFiles()?.forEach { file ->
                    zos.putNextEntry(ZipEntry("AppLogs/${file.name}"))
                    file.inputStream().copyTo(zos)
                    zos.closeEntry()
                }
                // 将内存中的 Logcat 打包
                zos.putNextEntry(ZipEntry("SystemLogcat.txt"))
                zos.write(state.logcatLines.joinToString("\n").toByteArray())
                zos.closeEntry()
            }
            
            withContext(Dispatchers.Main) {
                val uri = FileProvider.getUriForFile(
                    getApplication(),
                    "${getApplication<android.app.Application>().packageName}.fileprovider",
                    zipFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                getApplication<android.app.Application>().startActivity(
                    Intent.createChooser(intent, "分享日志").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                )
                LogStateHolder.state.update { it.copy(isExporting = false) }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                showToast("导出失败: ${e.message}")
                LogStateHolder.state.update { it.copy(isExporting = false) }
            }
        }
    }
}