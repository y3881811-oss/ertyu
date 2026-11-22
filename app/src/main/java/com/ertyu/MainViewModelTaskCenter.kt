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
