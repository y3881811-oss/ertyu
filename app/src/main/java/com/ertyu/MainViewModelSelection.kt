package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.model.ConflictAction
import com.ertyu.model.FileConflictState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

// 全局冲突状态
private val _fileConflictState = MutableStateFlow(FileConflictState())
val MainViewModel.fileConflictState: StateFlow<FileConflictState> get() = _fileConflictState

fun MainViewModel.clearConflictState() {
    _fileConflictState.value = FileConflictState()
}

/**
 * 👇 文件名严格校验
 * @return 如果校验通过返回 null，否则返回错误信息
 */
fun MainViewModel.validateFileName(
    isLeft: Boolean,
    newName: String,
    oldName: String? = null
): String? {
    if (newName.isBlank()) return "名称不能为空"
    if (newName.contains("/") || newName.contains("\\")) return "名称不能包含斜杠"
    if (newName.length > 255) return "名称过长"
    
    val state = flowOf(isLeft).value
    val hasSameName = state.items.any { 
        it.name.equals(newName, ignoreCase = true) && it.name != oldName 
    }
    if (hasSameName) return "当前目录下已存在同名文件或文件夹"
    
    return null
}

fun MainViewModel.batchCopy(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    // 获取目标面板
    val targetState = flowOf(!isLeft).value
    val targetDirPath = targetState.currentUri?.path ?: return

    // 👇 同名冲突检测
    val targetNames = targetState.items.map { it.name }.toSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        // 发现冲突，弹出拦截 UI
        _fileConflictState.value = FileConflictState(
            visible = true,
            conflictingNames = conflicts.map { it.name },
            onResolve = { action ->
                when (action) {
                    ConflictAction.CANCEL -> clearSelection(isLeft)
                    else -> {
                        // TODO: 将 action 传给底层的复制逻辑
                        // 示例：executeCopy(targets, targetDirPath, action)
                        clearSelection(isLeft)
                    }
                }
            }
        )
    } else {
        // 无冲突，直接复制
        // TODO: executeCopy(targets, targetDirPath, ConflictAction.OVERWRITE)
        clearSelection(isLeft)
    }
}

fun MainViewModel.batchMove(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    val targetState = flowOf(!isLeft).value
    val targetDirPath = targetState.currentUri?.path ?: return

    // 👇 同名冲突检测
    val targetNames = targetState.items.map { it.name }.toSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(
            visible = true,
            conflictingNames = conflicts.map { it.name },
            onResolve = { action ->
                when (action) {
                    ConflictAction.CANCEL -> clearSelection(isLeft)
                    else -> {
                        // TODO: 将 action 传给底层的移动逻辑
                        // 示例：executeMove(targets, targetDirPath, action)
                        clearSelection(isLeft)
                    }
                }
            }
        )
    } else {
        // TODO: executeMove(targets, targetDirPath, ConflictAction.OVERWRITE)
        clearSelection(isLeft)
    }
}