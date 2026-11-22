package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.DiffKind
import com.ertyu.engine.DirComparator
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.DirCompareHolder
import com.ertyu.model.DirCompareState
import kotlinx.coroutines.Job
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
fun MainViewModel.syncDirDiffs(toRight: Boolean) {
    val snap = DirCompareHolder.state.value
    if (snap.isSyncing) return
    val targets = snap.targets(toRight)
    if (targets.isEmpty()) {
        showToast("没有可同步的项")
        return
    }
    val leftRoot = snap.leftRoot
    val rightRoot = snap.rightRoot

    DirCompareJobs.syncJob = viewModelScope.launch {
        DirCompareHolder.state.update {
            it.copy(isSyncing = true, progressText = "正在同步 0/${targets.size}")
        }
        try {
            val (ok, fail) = DirComparator.copyTo(targets, toRight) { done, total ->
                DirCompareHolder.state.update { it.copy(progressText = "正在同步 $done/$total") }
            }
            AppLogger.i("DirCompare", "同步 ${if (toRight) "左->右" else "右->左"}：成功 $ok 失败 $fail")
            DirCompareHolder.state.update { it.copy(isSyncing = false, selected = emptySet()) }
            showToast(if (fail == 0) "同步完成：$ok 项" else "同步完成：成功 $ok 项，失败 $fail 项")

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
