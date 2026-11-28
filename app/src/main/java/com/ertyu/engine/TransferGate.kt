package com.ertyu.engine

import kotlin.coroutines.cancellation.CancellationException

/**
 * 传输闸门：让复制/移动在拷贝循环里可暂停、可取消。
 * 引擎在读写循环里调用 [checkpoint]，暂停时原地等待，取消时抛 [TransferCancelled]。
 */
object TransferGate {

    @Volatile
    var paused: Boolean = false
    @Volatile
    var cancelRequested: Boolean = false

    class TransferCancelled : CancellationException("传输已取消")

    fun reset() {
        paused = false
        cancelRequested = false
    }

    fun requestCancel() {
        cancelRequested = true
    }

    /** 拷贝循环里的检查点：暂停则阻塞，取消则抛异常 */
    fun checkpoint() {
        if (!paused && !cancelRequested) return
        while (paused) {
            if (cancelRequested) throw TransferCancelled()
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw TransferCancelled()
            }
        }
        if (cancelRequested) throw TransferCancelled()
    }
}
