package com.ertyu

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.SplitEngine
import com.ertyu.model.FileItem
import com.ertyu.model.SplitHolder
import com.ertyu.model.SplitState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openSplitDialog(item: FileItem) {
    val path = item.uri.path ?: run { showToast("无效路径"); return }
    SplitHolder.state.value = SplitState(
        visible = true,
        isMerge = false,
        targetPath = path,
        targetName = item.name,
        partSizeMb = 50
    )
}

fun MainViewModel.openMergeDialog(item: FileItem) {
    val path = item.uri.path ?: run { showToast("无效路径"); return }
    SplitHolder.state.value = SplitState(
        visible = true,
        isMerge = true,
        targetPath = path,
        targetName = item.name
    )
}

fun MainViewModel.closeSplitDialog() {
    if (SplitHolder.state.value.running) return
    SplitHolder.state.value = SplitState()
}

fun MainViewModel.setSplitPartSize(mb: Int) {
    SplitHolder.state.update { it.copy(partSizeMb = mb) }
}

fun MainViewModel.confirmSplit() {
    val s = SplitHolder.state.value
    if (s.running || s.targetPath.isBlank()) return
    val app = getApplication<Application>()
    SplitHolder.state.update { it.copy(running = true, progress = 0f, message = "准备中…") }

    viewModelScope.launch {
        if (s.isMerge) {
            val part = File(s.targetPath)
            val parts = SplitEngine.siblingsOf(part)
            val baseName = part.name.substringBeforeLast('.')
            val target = File(part.parentFile, baseName)
            if (target.exists()) {
                SplitHolder.state.update { it.copy(running = false, message = "同名文件已存在：${target.name}") }
                showToast("同名文件已存在，已取消合并")
                return@launch
            }
            val r = SplitEngine.merge(parts, target) { p ->
                SplitHolder.state.update { it.copy(progress = p, message = "合并中 ${(p * 100).toInt()}%") }
            }
            r.onSuccess {
                AppLogger.i("Split", "合并成功 ${target.absolutePath}")
                SplitHolder.state.value = SplitState()
                showToast("已合并为 ${target.name}")
                refresh(true); refresh(false)
            }.onFailure { e ->
                SplitHolder.state.update { it.copy(running = false, message = "合并失败：${e.message}") }
                AppLogger.e("Split", "合并失败", e)
            }
        } else {
            val source = File(s.targetPath)
            val partBytes = s.partSizeMb.toLong() * 1024 * 1024
            val r = SplitEngine.split(source, partBytes) { p ->
                SplitHolder.state.update { it.copy(progress = p, message = "分卷中 ${(p * 100).toInt()}%") }
            }
            r.onSuccess { files ->
                AppLogger.i("Split", "分卷成功 ${files.size} 卷")
                SplitHolder.state.value = SplitState()
                showToast("已分成 ${files.size} 卷")
                refresh(true); refresh(false)
            }.onFailure { e ->
                SplitHolder.state.update { it.copy(running = false, message = "分卷失败：${e.message}") }
                AppLogger.e("Split", "分卷失败", e)
            }
        }
    }
}
