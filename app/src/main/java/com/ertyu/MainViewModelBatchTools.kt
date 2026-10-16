package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.BatchRenamer
import com.ertyu.engine.FileChecksum
import com.ertyu.model.BatchRenameState
import com.ertyu.model.ChecksumState
import com.ertyu.model.FileItem
import com.ertyu.model.RenamePreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==================== 批量重命名 ====================

fun MainViewModel.openBatchRename(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter {
        state.selectedUris.contains(it.uriString) && !it.isDirectory
    }
    if (targets.isEmpty()) {
        _toast.value = "请先选择文件（不支持文件夹）"
        return
    }
    val initial = BatchRenameState(
        visible = true,
        sourceIsLeft = isLeft,
        targets = targets
    )
    BatchToolsState.batchRenameState.value = initial.copy(
        previews = computePreviews(targets, initial)
    )
}

fun MainViewModel.closeBatchRename() {
    BatchToolsState.batchRenameState.value = BatchRenameState()
}

fun MainViewModel.updateBatchRename(
    transform: (BatchRenameState) -> BatchRenameState
) {
    BatchToolsState.batchRenameState.update { current ->
        val updated = transform(current)
        updated.copy(previews = computePreviews(updated.targets, updated))
    }
}

private fun computePreviews(
    targets: List<FileItem>,
    state: BatchRenameState
): List<RenamePreview> {
    return targets.mapIndexed { idx, item ->
        val newName = BatchRenamer.computeName(
            original = item.name,
            index = idx,
            prefix = state.prefix,
            suffix = state.suffix,
            findText = state.findText,
            replaceText = state.replaceText,
            useRegex = state.useRegex,
            useNumbering = state.useNumbering,
            numberStart = state.numberStart,
            numberStep = state.numberStep,
            numberDigits = state.numberDigits,
            numberPosition = state.numberPosition
        )
        RenamePreview(item.name, newName)
    }
}

fun MainViewModel.executeBatchRename() {
    val state = BatchToolsState.batchRenameState.value
    if (!state.canExecute) return
    val isLeft = state.sourceIsLeft
    BatchToolsState.batchRenameState.update { it.copy(isExecuting = true) }

    viewModelScope.launch {
        var success = 0
        var failed = 0
        withContext(Dispatchers.IO) {
            state.targets.forEachIndexed { idx, item ->
                val newName = state.previews.getOrNull(idx)?.newName
                    ?: return@forEachIndexed
                val path = item.uri.path ?: return@forEachIndexed
                val src = File(path)
                val dst = File(src.parentFile, newName)
                if (dst.exists() && dst.absolutePath != src.absolutePath) {
                    failed++
                    return@forEachIndexed
                }
                if (src.renameTo(dst)) success++ else failed++
            }
        }
        BatchToolsState.batchRenameState.value = BatchRenameState()
        _toast.value = "重命名完成：成功 $success，失败 $failed"
        clearSelection(isLeft)
        refresh(isLeft)
    }
}

// ==================== 校验和 ====================

fun MainViewModel.openChecksum(item: FileItem) {
    val path = item.uri.path ?: return
    val file = File(path)
    if (!file.isFile) {
        _toast.value = "只支持文件"
        return
    }
    BatchToolsState.checksumState.value = ChecksumState(
        visible = true,
        fileName = file.name,
        filePath = file.absolutePath,
        fileSize = file.length(),
        isCalculating = true
    )
    calculateChecksums(file)
}

fun MainViewModel.closeChecksum() {
    BatchToolsState.checksumState.value = ChecksumState()
}

private fun MainViewModel.calculateChecksums(file: File) {
    viewModelScope.launch {
        try {
            val md5 = FileChecksum.calculate(file, "MD5") { p ->
                BatchToolsState.checksumState.update { it.copy(progress = p) }
            } ?: ""
            BatchToolsState.checksumState.update { it.copy(md5 = md5) }

            val sha1 = FileChecksum.calculate(file, "SHA-1") { p ->
                BatchToolsState.checksumState.update { it.copy(progress = p) }
            } ?: ""
            BatchToolsState.checksumState.update { it.copy(sha1 = sha1) }

            val sha256 = FileChecksum.calculate(file, "SHA-256") { p ->
                BatchToolsState.checksumState.update { it.copy(progress = p) }
            } ?: ""
            BatchToolsState.checksumState.update {
                it.copy(
                    sha256 = sha256,
                    isCalculating = false,
                    progress = 1f
                )
            }
        } catch (e: Exception) {
            BatchToolsState.checksumState.update {
                it.copy(isCalculating = false, error = e.message ?: "计算失败")
            }
        }
    }
}

fun MainViewModel.recalcChecksum() {
    val state = BatchToolsState.checksumState.value
    val file = File(state.filePath)
    if (!file.exists()) return
    BatchToolsState.checksumState.update {
        it.copy(
            isCalculating = true,
            md5 = "",
            sha1 = "",
            sha256 = "",
            error = null,
            progress = 0f
        )
    }
    calculateChecksums(file)
}