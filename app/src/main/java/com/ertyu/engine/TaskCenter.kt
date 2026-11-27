package com.ertyu.engine

import com.ertyu.model.TaskCenterHolder
import com.ertyu.model.TaskItem
import com.ertyu.model.TaskStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update

/**
 * 统一的任务记录：复制/移动/压缩/解压/上传下载/更新下载都往这里报一条。
 * 只做记录与“取消”（需要调用方把 Job 传进来），不改变原有执行逻辑。
 */
object TaskCenter {

    private var seq = 0L
    private val jobs = HashMap<Long, Job>()
    private val retries = HashMap<Long, () -> Unit>()

    fun start(
        title: String,
        kind: String,
        job: Job? = null,
        pausable: Boolean = false,
        retry: (() -> Unit)? = null
    ): Long {
        val id = ++seq
        if (job != null) jobs[id] = job
        if (retry != null) retries[id] = retry
        TaskCenterHolder.state.update { list ->
            (listOf(
                TaskItem(
                    id = id,
                    title = title,
                    kind = kind,
                    startedAt = System.currentTimeMillis(),
                    pausable = pausable,
                    retryable = retry != null
                )
            ) + list).take(100)
        }
        return id
    }

    fun progress(id: Long, fraction: Float?, detail: String = "") {
        TaskCenterHolder.state.update { list ->
            list.map { if (it.id == id) it.copy(progress = fraction, detail = detail) else it }
        }
    }

    fun finish(id: Long, ok: Boolean, detail: String = "") {
        jobs.remove(id)
        TaskCenterHolder.state.update { list ->
            list.map {
                if (it.id != id) it
                else it.copy(
                    status = if (ok) TaskStatus.DONE else TaskStatus.FAILED,
                    detail = detail.ifBlank { if (ok) "完成" else "失败" },
                    finishedAt = System.currentTimeMillis(),
                    progress = if (ok) 1f else it.progress
                )
            }
        }
    }

    /** 重新执行一条失败任务；返回 false 表示这条任务没有可重试的动作 */
    fun retry(id: Long): Boolean {
        val action = retries[id] ?: return false
        retries.remove(id)
        TaskCenterHolder.state.update { list ->
            list.map { if (it.id == id) it.copy(retryable = false, detail = it.detail + "（已重试）") else it }
        }
        action()
        return true
    }

    fun cancel(id: Long) {
        // 阻塞式拷贝靠闸门中止，光取消协程不够
        TransferGate.requestCancel()
        jobs.remove(id)?.cancel()
        TaskCenterHolder.state.update { list ->
            list.map {
                if (it.id == id && it.status == TaskStatus.RUNNING) it.copy(
                    status = TaskStatus.CANCELLED,
                    finishedAt = System.currentTimeMillis(),
                    detail = "已取消"
                ) else it
            }
        }
    }

    fun dismissFinished() {
        TaskCenterHolder.state.update { list -> list.filter { it.status == TaskStatus.RUNNING } }
    }

    fun clearAll() {
        TaskCenterHolder.state.value = emptyList()
    }
}
