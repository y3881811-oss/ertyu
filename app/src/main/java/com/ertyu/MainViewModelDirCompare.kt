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
    var job: Job? = null
}

fun MainViewModel.openDirCompare() {
    val left = leftPanel.value.currentUri?.path
    val right = rightPanel.value.currentUri?.path
    if (left.isNullOrBlank() || right.isNullOrBlank()) {
        showToast("请先让左右两个面板都打开一个目录")
        return
    }
    DirCompareHolder.state.value = DirCompareState(
        visible = true,
        leftRoot = left,
        rightRoot = right
    )
    startDirCompare()
}

fun MainViewModel.closeDirCompare() {
    DirCompareJobs.job?.cancel()
    DirCompareJobs.job = null
    DirCompareHolder.state.value = DirCompareState()
}

fun MainViewModel.setDirCompareDeep(deep: Boolean) {
    DirCompareHolder.state.update { it.copy(deep = deep) }
    startDirCompare()
}

fun MainViewModel.setDirCompareOnlyDiff(onlyDiff: Boolean) {
    DirCompareHolder.state.update { it.copy(onlyDiff = onlyDiff) }
}

fun MainViewModel.startDirCompare() {
    val snap = DirCompareHolder.state.value
    val left = File(snap.leftRoot)
    val right = File(snap.rightRoot)
    if (!left.isDirectory || !right.isDirectory) {
        DirCompareHolder.state.update { it.copy(error = "目录不可读，请重新选择") }
        return
    }
    DirCompareJobs.job?.cancel()
    DirCompareHolder.state.update {
        it.copy(isScanning = true, error = null, entries = emptyList(), selected = emptySet(), progressText = "正在建立索引…")
    }
    DirCompareJobs.job = viewModelScope.launch {
        try {
            val list = DirComparator.compare(left, right, snap.deep) { l, r ->
                DirCompareHolder.state.update { it.copy(progressText = "已索引 左 $l / 右 $r 项") }
            }
            val diffCount = list.count { it.kind != DiffKind.SAME }
            DirCompareHolder.state.update {
                it.copy(
                    isScanning = false,
                    entries = list,
                    progressText = "共 ${list.size} 项，其中差异 $diffCount 项"
                )
            }
            AppLogger.i("DirCompare", "对比完成：${list.size} 项，差异 $diffCount 项")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("DirCompare", "对比失败", e)
            DirCompareHolder.state.update { it.copy(isScanning = false, error = e.message ?: "对比失败") }
        } finally {
            DirCompareJobs.job = null
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

fun MainViewModel.clearDirDiffSelection() {
    DirCompareHolder.state.update { it.copy(selected = emptySet()) }
}

/** 把差异同步到另一侧 */
fun MainViewModel.syncDirDiffs(toRight: Boolean) {
    val snap = DirCompareHolder.state.value
    if (snap.isSyncing) return
    val pool = snap.entries.filter { it.kind != DiffKind.SAME }
    val chosen = if (snap.selected.isEmpty()) pool else pool.filter { it.relPath in snap.selected }
    // 左 -> 右 时，右侧独有的文件没有来源，跳过；反之同理
    val targets = if (toRight) chosen.filter { it.kind != DiffKind.RIGHT_ONLY }
    else chosen.filter { it.kind != DiffKind.LEFT_ONLY }
    if (targets.isEmpty()) {
        showToast("没有可同步的项（目标侧独有的项目需要选择反方向）")
        return
    }

    viewModelScope.launch {
        DirCompareHolder.state.update { it.copy(isSyncing = true, progressText = "正在同步 0/${targets.size}") }
        try {
            val (ok, fail) = DirComparator.copyTo(targets, toRight) { done, total ->
                DirCompareHolder.state.update { it.copy(progressText = "正在同步 $done/$total") }
            }
            showToast(if (fail == 0) "同步完成：$ok 项" else "同步完成：成功 $ok 项，失败 $fail 项")
            AppLogger.i("DirCompare", "同步 ${if (toRight) "左->右" else "右->左"}：成功 $ok 失败 $fail")
            DirCompareHolder.state.update { it.copy(isSyncing = false, selected = emptySet()) }
            // 刷新两侧面板与相关大小缓存
            val leftRoot = snap.leftRoot
            val rightRoot = snap.rightRoot
            invalidateSizeFor(leftRoot)
            invalidateSizeFor(rightRoot)
            ItemSizeEngine.invalidateUnder(leftRoot)
            ItemSizeEngine.invalidateUnder(rightRoot)
            refresh(true)
            refresh(false)
            startDirCompare()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("DirCompare", "同步失败", e)
            DirCompareHolder.state.update { it.copy(isSyncing = false, error = e.message ?: "同步失败") }
        }
    }
}
