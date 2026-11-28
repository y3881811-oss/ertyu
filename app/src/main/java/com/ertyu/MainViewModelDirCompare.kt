package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.DiffKind
import com.ertyu.engine.DirComparator
import com.ertyu.engine.RecycleBinManager
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.DirCompareHolder
import com.ertyu.model.DirCompareState
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private object DirCompareJobs {
    var scanJob: Job? = null
    var syncJob: Job? = null
}

fun MainViewModel.openDirCompare() {
    val left = leftPanel.value.currentUri?.path
    val right = rightPanel.value.currentUri?.path
    if (left.isNullOrBlank() || right.isNullOrBlank()) {
        showToast("请先让左右两个面板各打开一个目录")
        return
    }
    DirCompareHolder.state.value = DirCompareState(visible = true, leftRoot = left, rightRoot = right)
    startDirCompare()
}

/** 关闭页面：如果正在同步，先提示再由用户决定 */
fun MainViewModel.closeDirCompare() {
    val syncing = DirCompareJobs.syncJob?.isActive == true
    DirCompareJobs.scanJob?.cancel()
    DirCompareJobs.syncJob?.cancel()
    DirCompareJobs.scanJob = null
    DirCompareJobs.syncJob = null
    DirCompareHolder.state.value = DirCompareState()
    if (syncing) showToast("同步已中止（已复制的文件不会回滚）")
}

fun MainViewModel.setDirCompareDeep(deep: Boolean) {
    if (DirCompareHolder.state.value.deep == deep) return
    DirCompareHolder.state.update { it.copy(deep = deep) }
    startDirCompare()
}

fun MainViewModel.setDirCompareOnlyDiff(onlyDiff: Boolean) {
    DirCompareHolder.state.update { it.copy(onlyDiff = onlyDiff) }
}

fun MainViewModel.startDirCompare() {
    if (DirCompareJobs.syncJob?.isActive == true) {
        showToast("正在同步，完成后再重新对比")
        return
    }
    val snap = DirCompareHolder.state.value
    val left = File(snap.leftRoot)
    val right = File(snap.rightRoot)
    if (!left.isDirectory || !right.isDirectory) {
        DirCompareHolder.state.update { it.copy(error = "目录不可读，请重新打开两侧目录") }
        return
    }
    DirCompareJobs.scanJob?.cancel()
    DirCompareHolder.state.update {
        it.copy(
            isScanning = true, error = null, entries = emptyList(),
            selected = emptySet(), progressText = "正在建立索引…"
        )
    }
    DirCompareJobs.scanJob = viewModelScope.launch {
        try {
            val list = DirComparator.compare(left, right, snap.deep) { l, r ->
                DirCompareHolder.state.update { it.copy(progressText = "已索引 左 $l / 右 $r 项") }
            }
            val diffCount = list.count { it.kind != DiffKind.SAME }
            DirCompareHolder.state.update {
                it.copy(
                    isScanning = false,
                    entries = list,
                    progressText = if (diffCount == 0) "两侧完全一致" else "共 ${list.size} 项，差异 $diffCount 项"
                )
            }
            AppLogger.i("DirCompare", "对比完成：${list.size} 项，差异 $diffCount 项")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("DirCompare", "对比失败", e)
            DirCompareHolder.state.update { it.copy(isScanning = false, error = e.message ?: "对比失败") }
        } finally {
            DirCompareJobs.scanJob = null
        }
    }
}

fun MainViewModel.toggleDirDiffSelection(relPath: String) {
    DirCompareHolder.state.update {
        val set = it.selected.toMutableSet()
        if (!set.add(relPath)) set.remove(relPath)
        it.copy(selected = set)
    }
}

fun MainViewModel.selectAllDirDiffs() {
    DirCompareHolder.state.update {
        it.copy(selected = it.visibleEntries.filter { e -> e.kind != DiffKind.SAME }.map { e -> e.relPath }.toSet())
    }
}

fun MainViewModel.invertDirDiffSelection() {
    DirCompareHolder.state.update {
        val all = it.visibleEntries.filter { e -> e.kind != DiffKind.SAME }.map { e -> e.relPath }.toSet()
        it.copy(selected = all - it.selected)
    }
}

fun MainViewModel.clearDirDiffSelection() {
    DirCompareHolder.state.update { it.copy(selected = emptySet()) }
}

fun MainViewModel.cancelDirDiffSync() {
    if (DirCompareJobs.syncJob?.isActive != true) return
    DirCompareJobs.syncJob?.cancel()
    DirCompareJobs.syncJob = null
    DirCompareHolder.state.update { it.copy(isSyncing = false, progressText = "同步已停止") }
    showToast("已停止同步（已复制的文件不会回滚）")
}

/**
 * 执行同步。目标由 state.targets() 决定，和确认弹窗里显示的数字一致。
 */
fun MainViewModel.syncDirDiffs(toRight: Boolean, mirror: Boolean = false) {
    val snap = DirCompareHolder.state.value
    if (snap.isSyncing) return
    val targets = snap.targets(toRight)
    val extras = if (mirror) snap.extras(toRight) else emptyList()
    if (targets.isEmpty() && extras.isEmpty()) {
        showToast("没有可同步的项")
        return
    }
    val leftRoot = snap.leftRoot
    val rightRoot = snap.rightRoot
    val useTrash = _settingsState.value.useRecycleBin

    DirCompareJobs.syncJob = viewModelScope.launch {
        DirCompareHolder.state.update {
            it.copy(isSyncing = true, progressText = "正在同步 0/${targets.size}")
        }
        try {
            val (ok, fail) = DirComparator.copyTo(targets, toRight) { done, total ->
                DirCompareHolder.state.update { it.copy(progressText = "正在复制 $done/$total") }
            }

            // 镜像模式：删除目标侧独有的项
            var delOk = 0
            var delFail = 0
            if (mirror && extras.isNotEmpty()) {
                DirCompareHolder.state.update { it.copy(progressText = "正在清理目标侧多余项 0/${extras.size}") }
                for ((index, e) in extras.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val f = File(if (toRight) e.rightPath else e.leftPath)
                    if (!f.exists()) continue
                    val size = if (e.isDirectory) 0L else (try { f.length() } catch (_: Exception) { 0L })
                    val success = try {
                        if (useTrash) RecycleBinManager.moveToTrash(getApplication(), f, e.isDirectory, size)
                        else if (f.isDirectory) f.deleteRecursively() else f.delete()
                    } catch (ex: Exception) {
                        AppLogger.e("DirCompare", "删除失败 ${f.absolutePath}", ex)
                        false
                    }
                    if (success) delOk++ else delFail++
                    DirCompareHolder.state.update {
                        it.copy(progressText = "正在清理目标侧多余项 ${index + 1}/${extras.size}")
                    }
                }
            }

            AppLogger.i(
                "DirCompare",
                "同步 ${if (toRight) "左->右" else "右->左"}：复制成功 $ok 失败 $fail" +
                    if (mirror) "；删除成功 $delOk 失败 $delFail（回收站=$useTrash）" else ""
            )
            DirCompareHolder.state.update { it.copy(isSyncing = false, selected = emptySet()) }
            val copyText = if (fail == 0) "复制 $ok 项" else "复制成功 $ok 项/失败 $fail 项"
            val delText = if (mirror) "，${if (useTrash) "移入回收站" else "删除"} $delOk 项" +
                if (delFail > 0) "（失败 $delFail 项）" else "" else ""
            showToast("同步完成：$copyText$delText")

            invalidateSizeFor(leftRoot)
            invalidateSizeFor(rightRoot)
            ItemSizeEngine.invalidateUnder(leftRoot)
            ItemSizeEngine.invalidateUnder(rightRoot)
            refresh(true)
            refresh(false)
            startDirCompare()
        } catch (e: kotlinx.coroutines.CancellationException) {
            DirCompareHolder.state.update { it.copy(isSyncing = false) }
            throw e
        } catch (e: Exception) {
            AppLogger.e("DirCompare", "同步失败", e)
            DirCompareHolder.state.update { it.copy(isSyncing = false, error = e.message ?: "同步失败") }
        } finally {
            DirCompareJobs.syncJob = null
        }
    }
}
