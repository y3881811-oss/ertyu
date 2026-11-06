package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.FileEngine
import com.ertyu.model.ConflictAction
import com.ertyu.model.FileConflictState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ===== 冲突状态管理 =====
private val _fileConflictState = MutableStateFlow(FileConflictState())
val MainViewModel.fileConflictState: StateFlow<FileConflictState> get() = _fileConflictState

fun MainViewModel.clearConflictState() {
    _fileConflictState.value = FileConflictState()
}

/**
 * 文件名严格校验
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

/**
 * 生成不重复的文件名（保留两者策略）
 * 例如：a.txt -> a (1).txt -> a (2).txt
 */
private fun generateUniqueFileName(name: String, existingNames: Set<String>): String {
    if (!existingNames.contains(name)) return name
    
    // 简单判断是否为文件夹（无后缀）
    val isDirectory = !name.contains(".")
    val baseName = if (isDirectory) name else name.substringBeforeLast(".")
    val extension = if (isDirectory) "" else ".${name.substringAfterLast(".")}"
    
    var counter = 1
    var newName = "$baseName ($counter)$extension"
    while (existingNames.contains(newName)) {
        counter++
        newName = "$baseName ($counter)$extension"
    }
    return newName
}

/**
 * 获取引擎的辅助方法（如果你有专门的方法，请替换此处）
 */
private fun MainViewModel.getEngine(isLeft: Boolean): FileEngine {
    // TODO: 根据你的架构替换。例如：
    // return if (isLeft) leftEngine else rightEngine
    // 或者 return engineFor(isLeft)
    return engine
}

fun MainViewModel.batchCopy(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    val targetState = flowOf(!isLeft).value
    val targetDirUri = targetState.currentUri ?: return

    val targetNames = targetState.items.map { it.name }.toMutableSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(
            visible = true,
            conflictingNames = conflicts.map { it.name },
            onResolve = { action ->
                if (action != ConflictAction.CANCEL) {
                    executeTransfer(isLeft, targets, targetDirUri, targetNames, action, isMove = false)
                } else {
                    clearSelection(isLeft)
                }
            }
        )
    } else {
        executeTransfer(isLeft, targets, targetDirUri, targetNames, ConflictAction.OVERWRITE, isMove = false)
    }
}

fun MainViewModel.batchMove(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return

    val targetState = flowOf(!isLeft).value
    val targetDirUri = targetState.currentUri ?: return

    val targetNames = targetState.items.map { it.name }.toMutableSet()
    val conflicts = targets.filter { targetNames.contains(it.name) }

    if (conflicts.isNotEmpty()) {
        _fileConflictState.value = FileConflictState(
            visible = true,
            conflictingNames = conflicts.map { it.name },
            onResolve = { action ->
                if (action != ConflictAction.CANCEL) {
                    executeTransfer(isLeft, targets, targetDirUri, targetNames, action, isMove = true)
                } else {
                    clearSelection(isLeft)
                }
            }
        )
    } else {
        executeTransfer(isLeft, targets, targetDirUri, targetNames, ConflictAction.OVERWRITE, isMove = true)
    }
}

/**
 * 核心执行逻辑
 */
private fun MainViewModel.executeTransfer(
    isLeft: Boolean,
    targets: List<FileItem>,
    targetDirUri: Uri,
    targetNames: MutableSet<String>,
    action: ConflictAction,
    isMove: Boolean
) {
    viewModelScope.launch {
        val sourceEngine = getEngine(isLeft)
        val targetEngine = getEngine(!isLeft) 
        
        var successCount = 0
        var skipCount = 0

        _toast.value = if (isMove) "正在移动..." else "正在复制..."

        for (item in targets) {
            var conflictItem = flowOf(!isLeft).value.items.find { it.name == item.name }
            
            if (conflictItem != null) {
                when (action) {
                    ConflictAction.SKIP -> {
                        skipCount++
                        continue
                    }
                    ConflictAction.OVERWRITE -> {
                        // 覆盖：先删除目标目录的冲突文件
                        targetEngine.delete(conflictItem.uri)
                        targetNames.remove(conflictItem.name)
                    }
                    ConflictAction.KEEP_BOTH -> {
                        // 保留两者：给目标目录的冲突文件改个名，腾出位置
                        val newName = generateUniqueFileName(conflictItem.name, targetNames)
                        targetEngine.rename(conflictItem.uri, newName)
                        targetNames.remove(conflictItem.name)
                        targetNames.add(newName)
                    }
                    else -> {}
                }
            }

            try {
                // 执行复制或移动
                val success = if (isMove) {
                    sourceEngine.move(item.uri, targetDirUri)
                } else {
                    sourceEngine.copy(item.uri, targetDirUri)
                }
                
                if (success) {
                    targetNames.add(item.name) // 更新目标目录名字集合
                    successCount++
                }
            } catch (e: Exception) {
                // 忽略单个文件的失败，继续处理下一个
            }
        }
        
        _toast.value = "${if (isMove) "移动" else "复制"}完成：成功 $successCount 项" + 
                if (skipCount > 0) "，跳过 $skipCount 项" else ""
        
        clearSelection(isLeft)
        refresh(isLeft)   // 刷新源面板
        refresh(!isLeft)  // 刷新目标面板
        clearConflictState()
    }
}