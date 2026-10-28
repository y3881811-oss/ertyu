package com.ertyu.model

data class TerminalLine(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false
)

data class TerminalState(
    val visible: Boolean = false,
    val lines: List<TerminalLine> = emptyList(),
    val currentInput: String = "",
    val isExecuting: Boolean = false,
    val useRoot: Boolean = false,
    val cwd: String = "/storage/emulated/0"
)

data class ProcessInfo(
    val pid: String,
    val user: String,
    val cpu: String,
    val mem: String,
    val name: String
)

data class ProcessManagerState(
    val visible: Boolean = false,
    val processes: List<ProcessInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)