package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

data class LogState(
    val visible: Boolean = false,
    val logs: List<LogEntry> = emptyList(),
    val logcatLines: List<String> = emptyList(), // 👈 将 Logcat 按行拆分，方便 LazyColumn 渲染
    val searchQuery: String = "",
    val filterLevel: String? = null,
    val isExporting: Boolean = false,
    val isLoading: Boolean = false
) {
    val filteredLogs: List<LogEntry>
        get() = logs.filter {
            (filterLevel == null || it.level == filterLevel) &&
            (searchQuery.isEmpty() || it.message.contains(searchQuery, ignoreCase = true) || it.module.contains(searchQuery, ignoreCase = true))
        }
}

data class LogEntry(val time: String, val level: String, val module: String, val message: String)

object LogStateHolder {
    val state = MutableStateFlow(LogState())
}