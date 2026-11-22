package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.RecycleBinManager
import com.ertyu.model.RecycleBinItem
import com.ertyu.model.RecycleBinState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun MainViewModel.openRecycleBin() {
    val keepSelection = RecycleBinStateHolder.state.value.selected
    RecycleBinStateHolder.state.value = RecycleBinState(visible = true, isLoading = true, selected = keepSelection)
    viewModelScope.launch {
        val items = RecycleBinManager.getItems(getApplication())
        RecycleBinStateHolder.state.update { it.copy(items = items, isLoading = false) }
    }
}

fun MainViewModel.closeRecycleBin() {
    RecycleBinStateHolder.state.value = RecycleBinState()
}

fun MainViewModel.refreshRecycleBin() {
    viewModelScope.launch {
        val items = RecycleBinManager.getItems(getApplication())
        RecycleBinStateHolder.state.update {
            it.copy(items = items, selected = it.selected.filter { id -> items.any { i -> i.id == id } }.toSet())
        }
    }
}

// ==================== 选择 ====================

fun MainViewModel.toggleRecycleSelection(id: String) {
    RecycleBinStateHolder.state.update {
        val set = it.selected.toMutableSet()
        if (!set.add(id)) set.remove(id)
        it.copy(selected = set)
    }
}

fun MainViewModel.selectAllRecycleItems() {
    RecycleBinStateHolder.state.update { it.copy(selected = it.items.map { i -> i.id }.toSet()) }
}

fun MainViewModel.clearRecycleSelection() {
    RecycleBinStateHolder.state.update { it.copy(selected = emptySet()) }
}

// ==================== 单个操作 ====================

fun MainViewModel.restoreFromTrash(item: RecycleBinItem) {
    viewModelScope.launch {
        val (ok, message) = RecycleBinManager.restoreSafely(getApplication(), item)
        showToast(message ?: if (ok) "已还原：${item.name}" else "还原失败，原路径可能被占用")
        afterRecycleChange()
    }
}

fun MainViewModel.deleteFromTrash(item: RecycleBinItem) {
    viewModelScope.launch {
        RecycleBinManager.deletePermanently(getApplication(), item)
        showToast("已彻底删除：${item.name}")
        afterRecycleChange()
    }
}

fun MainViewModel.clearRecycleBin() {
    viewModelScope.launch {
        RecycleBinManager.clearAll(getApplication())
        RecycleBinStateHolder.state.update { it.copy(items = emptyList(), selected = emptySet()) }
        showToast("回收站已清空")
        afterRecycleChange(alreadyCleared = true)
    }
}

// ==================== 批量操作 ====================

fun MainViewModel.restoreSelectedRecycleItems() {
    val snap = RecycleBinStateHolder.state.value
    val targets = snap.selectedItems()
    if (targets.isEmpty() || snap.working) return
    viewModelScope.launch {
        RecycleBinStateHolder.state.update { it.copy(working = true) }
        var ok = 0
        var renamed = 0
        targets.forEach { item ->
            val (success, message) = RecycleBinManager.restoreSafely(getApplication(), item)
            if (success) {
                ok++
                if (message != null) renamed++
            }
        }
        RecycleBinStateHolder.state.update { it.copy(working = false, selected = emptySet()) }
        showToast(
            when {
                ok == targets.size && renamed == 0 -> "已还原 $ok 项"
                ok == targets.size -> "已还原 $ok 项（其中 $renamed 项因重名自动改名）"
                else -> "还原完成：成功 $ok 项，失败 ${targets.size - ok} 项"
            }
        )
        afterRecycleChange()
    }
}

fun MainViewModel.deleteSelectedRecycleItems() {
    val snap = RecycleBinStateHolder.state.value
    val targets = snap.selectedItems()
    if (targets.isEmpty() || snap.working) return
    viewModelScope.launch {
        RecycleBinStateHolder.state.update { it.copy(working = true) }
        targets.forEach { RecycleBinManager.deletePermanently(getApplication(), it) }
        RecycleBinStateHolder.state.update { it.copy(working = false, selected = emptySet()) }
        showToast("已彻底删除 ${targets.size} 项")
        afterRecycleChange()
    }
}

/**
 * 按时间清理：删掉早于 days 天前放入回收站的条目。
 * days <= 0 表示清空全部。
 */
fun MainViewModel.purgeRecycleOlderThan(days: Int) {
    val snap = RecycleBinStateHolder.state.value
    if (snap.working) return
    viewModelScope.launch {
        RecycleBinStateHolder.state.update { it.copy(working = true) }
        val removed = withContext(Dispatchers.IO) { RecycleBinManager.purgeOlderThan(getApplication(), days) }
        RecycleBinStateHolder.state.update { it.copy(working = false, selected = emptySet()) }
        showToast(if (removed == 0) "没有符合条件的条目" else "已清理 $removed 项")
        AppLogger.i("RecycleBin", "按时间清理 $days 天：$removed 项")
        afterRecycleChange()
    }
}

/** 回收站变化后：刷新列表、刷新两个面板、失效相关大小缓存 */
private fun MainViewModel.afterRecycleChange(alreadyCleared: Boolean = false) {
    if (!alreadyCleared) refreshRecycleBin()
    refresh(true)
    refresh(false)
    // 还原/删除都可能影响任意目录，保守地把内存中的大小结果清掉
    ItemSizeEngine.invalidate()
    com.ertyu.model.ItemSizeHolder.clearAll()
}
