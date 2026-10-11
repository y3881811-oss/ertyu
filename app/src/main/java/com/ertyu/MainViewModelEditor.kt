package com.ertyu

import kotlinx.coroutines.flow.update

// ==================== 查找 / 替换 ====================

fun MainViewModel.openFind() {
    _editor.update { it.copy(findVisible = true) }
}

fun MainViewModel.closeFind() {
    _editor.update {
        it.copy(
            findVisible = false,
            findQuery = "",
            replaceQuery = "",
            totalMatches = 0,
            currentMatchIndex = 0
        )
    }
}

fun MainViewModel.updateFindQuery(q: String) {
    _editor.update { st ->
        val matches = countMatches(st.content, q)
        st.copy(
            findQuery = q,
            totalMatches = matches,
            currentMatchIndex = if (matches > 0) 1 else 0
        )
    }
}

fun MainViewModel.updateReplaceQuery(q: String) {
    _editor.update { it.copy(replaceQuery = q) }
}

private fun countMatches(text: String, query: String): Int {
    if (query.isEmpty()) return 0
    var count = 0
    var idx = 0
    while (true) {
        val found = text.indexOf(query, idx, ignoreCase = false)
        if (found < 0) break
        count++
        idx = found + query.length
    }
    return count
}

fun MainViewModel.findNext() {
    val s = _editor.value
    if (s.findQuery.isEmpty() || s.totalMatches == 0) return
    val next = if (s.currentMatchIndex >= s.totalMatches) 1
    else s.currentMatchIndex + 1
    _editor.update { it.copy(currentMatchIndex = next) }
}

fun MainViewModel.findPrev() {
    val s = _editor.value
    if (s.findQuery.isEmpty() || s.totalMatches == 0) return
    val prev = if (s.currentMatchIndex <= 1) s.totalMatches
    else s.currentMatchIndex - 1
    _editor.update { it.copy(currentMatchIndex = prev) }
}

fun MainViewModel.replaceCurrent() {
    val s = _editor.value
    if (s.findQuery.isEmpty()) return
    val newContent = s.content.replaceFirst(s.findQuery, s.replaceQuery)
    val matches = countMatches(newContent, s.findQuery)
    _editor.update {
        it.copy(
            content = newContent,
            totalMatches = matches,
            currentMatchIndex = if (matches > 0) 1 else 0
        )
    }
}

fun MainViewModel.replaceAll() {
    val s = _editor.value
    if (s.findQuery.isEmpty()) return
    val newContent = s.content.replace(s.findQuery, s.replaceQuery)
    _editor.update {
        it.copy(
            content = newContent,
            totalMatches = 0,
            currentMatchIndex = 0
        )
    }
}

// ==================== 跳转 ====================

fun MainViewModel.openJumpDialog() {
    _editor.update { it.copy(jumpDialogVisible = true) }
}

fun MainViewModel.closeJumpDialog() {
    _editor.update { it.copy(jumpDialogVisible = false) }
}

/** 计算指定行首的字符偏移量 */
fun MainViewModel.lineToOffset(line: Int): Int {
    val s = _editor.value
    if (line <= 1) return 0
    var currentLine = 1
    var i = 0
    while (i < s.content.length && currentLine < line) {
        if (s.content[i] == '\n') currentLine++
        i++
    }
    return i
}

// ==================== 字号 ====================

fun MainViewModel.increaseFontSize() {
    _editor.update {
        it.copy(fontSize = (it.fontSize + 2f).coerceAtMost(32f))
    }
}

fun MainViewModel.decreaseFontSize() {
    _editor.update {
        it.copy(fontSize = (it.fontSize - 2f).coerceAtLeast(10f))
    }
}

// ==================== 光标位置 ====================

fun MainViewModel.updateCursorPosition(line: Int, column: Int) {
    _editor.update { it.copy(cursorLine = line, cursorColumn = column) }
}