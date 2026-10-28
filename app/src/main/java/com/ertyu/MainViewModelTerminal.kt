package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ShellExecutor
import com.ertyu.model.ProcessInfo
import com.ertyu.model.ProcessManagerState
import com.ertyu.model.TerminalLine
import com.ertyu.model.TerminalState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object TerminalStateHolder {
    val state = MutableStateFlow(TerminalState())
    val processState = MutableStateFlow(ProcessManagerState())
}

fun MainViewModel.openTerminal() {
    TerminalStateHolder.state.update {
        it.copy(visible = true, lines = listOf(TerminalLine("Welcome to MTFM Terminal\n", isCommand = false)))
    }
}

fun MainViewModel.closeTerminal() {
    TerminalStateHolder.state.value = TerminalState()
}

fun MainViewModel.updateTerminalInput(text: String) {
    TerminalStateHolder.state.update { it.copy(currentInput = text) }
}

fun MainViewModel.toggleRoot() {
    TerminalStateHolder.state.update { it.copy(useRoot = !it.useRoot) }
}

fun MainViewModel.executeTerminalCommand() {
    val state = TerminalStateHolder.state.value
    val cmd = state.currentInput.trim()
    if (cmd.isEmpty()) return

    val newLines = state.lines.toMutableList()
    newLines.add(TerminalLine("${if (state.useRoot) "root@" else "shell@"}${state.cwd} $ $cmd", isCommand = true))
    TerminalStateHolder.state.update { it.copy(lines = newLines, currentInput = "", isExecuting = true) }

    viewModelScope.launch {
        if (cmd.startsWith("cd ")) {
            val newPath = cmd.substring(3).trim()
            TerminalStateHolder.state.update { it.copy(cwd = newPath, isExecuting = false) }
            return@launch
        }

        val result = ShellExecutor.execute("cd '${state.cwd}' && $cmd", state.useRoot)
        val updatedLines = TerminalStateHolder.state.value.lines.toMutableList()
        if (result.isNotEmpty()) {
            updatedLines.add(TerminalLine(result, isError = result.startsWith("Error")))
        }
        TerminalStateHolder.state.update { it.copy(lines = updatedLines, isExecuting = false) }
    }
}

fun MainViewModel.openProcessManager() {
    TerminalStateHolder.processState.update { it.copy(visible = true, isLoading = true, error = null) }
    viewModelScope.launch {
        val raw = ShellExecutor.execute("top -n 1 -b", useRoot = false)
        if (raw.isBlank()) {
            TerminalStateHolder.processState.update { it.copy(isLoading = false, error = "无法获取进程列表") }
            return@launch
        }
        
        val list = mutableListOf<ProcessInfo>()
        val lines = raw.split("\n")
        var startParsing = false
        
        for (line in lines) {
            if (line.contains("PID") && line.contains("USER")) { startParsing = true; continue }
            if (!startParsing || line.isBlank()) continue
            
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 8) {
                list.add(ProcessInfo(parts[0], parts[1], parts[2], parts[5], parts.last()))
            }
        }
        TerminalStateHolder.processState.update { it.copy(isLoading = false, processes = list) }
    }
}

fun MainViewModel.closeProcessManager() {
    TerminalStateHolder.processState.value = ProcessManagerState()
}