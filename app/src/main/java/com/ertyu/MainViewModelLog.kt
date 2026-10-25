package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.LogManager
import com.ertyu.model.LogState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun MainViewModel.openLogViewer() {
    LogStateHolder.state.value = LogState(visible = true)
    refreshLogList()
}

fun MainViewModel.closeLogViewer() {
    LogStateHolder.state.value = LogState()
}

fun MainViewModel.captureLog() {
    if (LogStateHolder.state.value.isSaving) return
    LogStateHolder.state.update { it.copy(isSaving = true) }
    
    viewModelScope.launch {
        val result = LogManager.saveCurrentLog()
        result.onSuccess { file ->
            _toast.value = "日志已保存：${file.name}"
            refreshLogList()
        }.onFailure { e ->
            _toast.value = "抓取失败：${e.message}"
        }
        LogStateHolder.state.update { it.copy(isSaving = false) }
    }
}

fun MainViewModel.refreshLogList() {
    viewModelScope.launch {
        val files = LogManager.getLogFiles()
        LogStateHolder.state.update { it.copy(logFiles = files) }
    }
}

fun MainViewModel.openLogFile(item: com.ertyu.model.FileItem) {
    closeLogViewer()
    openInEditor(item)
}