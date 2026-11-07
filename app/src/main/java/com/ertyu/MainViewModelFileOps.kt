package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.FileEngine
import com.ertyu.model.ConflictAction
import com.ertyu.model.FileConflictState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 生成不重复的文件名（保留两者策略）
 */
private fun generateUniqueFileName(name: String, existingNames: Set<String>): String {
    if (!existingNames.contains(name)) return name
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
                        targetEngine.delete(conflictItem.uri)
                        targetNames.remove(conflictItem.name)
                    }
                    ConflictAction.KEEP_BOTH -> {
                        val newName = generateUniqueFileName(conflictItem.name, targetNames)
                        targetEngine.rename(conflictItem.uri, newName)
                        targetNames.remove(conflictItem.name)
                        targetNames.add(newName)
                    }
                    else -> {}
                }
            }

            try {
                val success = if (isMove) {
                    sourceEngine.move(item.uri, targetDirUri)
                } else {
                    sourceEngine.copy(item.uri, targetDirUri)
                }
                
                if (success) {
                    targetNames.add(item.name)
                    successCount++
                }
            } catch (e: Exception) {
                // 忽略
            }
        }
        
        _toast.value = "${if (isMove) "移动" else "复制"}完成：成功 $successCount 项" + 
                if (skipCount > 0) "，跳过 $skipCount 项" else ""
        
        clearSelection(isLeft)
        refresh(isLeft)
        refresh(!isLeft)
        clearConflictState()
    }
}

// 获取引擎（保留你原来的逻辑即可）
private fun MainViewModel.getEngine(isLeft: Boolean): FileEngine {
    return engine
}