package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.model.BatchMkdirHolder
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun MainViewModel.openBatchMkdir(isLeft: Boolean) {
    BatchMkdirHolder.state.value = com.ertyu.model.BatchMkdirState(visible = true, isLeft = isLeft)
}

fun MainViewModel.closeBatchMkdir() {
    BatchMkdirHolder.state.update { it.copy(visible = false, creating = false) }
}

/**
 * 批量新建文件夹：每行一个名字，忽略空行与重名。
 */
fun MainViewModel.createBatchFolders(names: List<String>) {
    val snap = BatchMkdirHolder.state.value
    if (snap.creating) return
    val parent = flowOf(snap.isLeft).value.currentUri
    if (parent == null) {
        showToast("当前目录还没就绪，请稍后再试")
        return
    }
    // 名字里的 / 会破坏目录层级，直接替换掉
    val cleaned = names
        .map { it.trim().replace("/", "_").replace("\\", "_") }
        .filter { it.isNotEmpty() }
        .distinct()
    if (cleaned.isEmpty()) {
        showToast("没有有效的文件夹名")
        return
    }

    BatchMkdirHolder.state.update { it.copy(creating = true) }
    viewModelScope.launch {
        var ok = 0
        var fail = 0
        for (name in cleaned) {
            val uri = try {
                engine.createDirectory(parent, name)
            } catch (e: Exception) {
                AppLogger.e("BatchMkdir", "创建失败 $name", e)
                null
            }
            if (uri != null) ok++ else fail++
        }
        BatchMkdirHolder.state.update { it.copy(visible = false, creating = false) }
        showToast(if (fail == 0) "已创建 $ok 个文件夹" else "创建完成：成功 $ok 个，失败 $fail 个")
        AppLogger.i("BatchMkdir", "批量新建 成功 $ok / 失败 $fail")
        refresh(snap.isLeft)
    }
}
