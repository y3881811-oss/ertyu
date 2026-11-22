package com.ertyu

import com.ertyu.engine.TaskCenter
import com.ertyu.model.TaskCenterHolder

fun MainViewModel.openTaskCenter() {
    TaskCenterHolder.visible.value = true
}

fun MainViewModel.closeTaskCenter() {
    TaskCenterHolder.visible.value = false
}

fun MainViewModel.cancelTask(id: Long) {
    TaskCenter.cancel(id)
    showToast("已请求取消该任务")
}

fun MainViewModel.dismissFinishedTasks() {
    TaskCenter.dismissFinished()
}

/** 暂停/继续 复制、移动 */
fun MainViewModel.toggleTransferPause() {
    val hasRunning = TaskCenterHolder.state.value.any { it.status == com.ertyu.model.TaskStatus.RUNNING && it.pausable }
    if (!hasRunning) {
        showToast("当前没有正在进行的复制/移动")
        return
    }
    val next = !com.ertyu.engine.TransferGate.paused
    com.ertyu.engine.TransferGate.paused = next
    TaskCenterHolder.paused.value = next
    showToast(if (next) "已暂停传输" else "已继续传输")
}
