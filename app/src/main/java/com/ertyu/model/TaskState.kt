package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

enum class TaskStatus(val label: String) {
    RUNNING("进行中"),
    DONE("完成"),
    FAILED("失败"),
    CANCELLED("已取消")
}

data class TaskItem(
    val id: Long,
    val title: String,
    val kind: String,
    val startedAt: Long,
    val status: TaskStatus = TaskStatus.RUNNING,
    val progress: Float? = null,
    val detail: String = "",
    val finishedAt: Long = 0L
)

object TaskCenterHolder {
    val state = MutableStateFlow<List<TaskItem>>(emptyList())
    val visible = MutableStateFlow(false)
}
