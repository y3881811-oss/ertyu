package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.model.DragDropHolder
import com.ertyu.model.PendingDragMove
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun MainViewModel.cancelDragMove() {
    DragDropHolder.pending.value = null
}

/**
 * 把拖拽过来的条目移动到对侧面板的当前目录。
 * 目标已存在同名文件时由引擎处理（通常是自动改名）。
 */
fun MainViewModel.confirmDragMove() {
    val p = DragDropHolder.pending.value ?: return
    DragDropHolder.pending.value = null
    val targetIsLeft = !p.fromLeft
    val targetUri = flowOf(targetIsLeft).value.currentUri
    if (targetUri == null) {
        showToast("对侧面板还没打开目录")
        return
    }
    viewModelScope.launch {
        var ok = 0
        var fail = 0
        for (uriStr in p.uris) {
            val uri = android.net.Uri.parse(uriStr)
            val success = try {
                engine.move(uri, targetUri)
            } catch (e: Exception) {
                AppLogger.e("DragMove", "移动失败 $uriStr", e)
                false
            }
            if (success) ok++ else fail++
        }
        showToast(if (fail == 0) "已移动 $ok 项" else "移动完成：成功 $ok 项，失败 $fail 项")
        AppLogger.i("DragMove", "拖拽移动 ${if (p.fromLeft) "左→右" else "右→左"}：成功 $ok 失败 $fail")
        refresh(p.fromLeft)
        refresh(targetIsLeft)
    }
}
