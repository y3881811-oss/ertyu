package com.ertyu.model

import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.flow.MutableStateFlow

/** 拖拽中携带的文件（同一面板的一次拖拽） */
data class DraggedPayload(
    val uris: List<String>,
    val names: List<String>,
    val fromLeft: Boolean
)

data class DragDropState(
    val active: Boolean = false,
    val payload: DraggedPayload? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val leftBounds: Rect? = null,
    val rightBounds: Rect? = null,
    val hoverLeft: Boolean = false,
    val hoverRight: Boolean = false
)

/** 松手后等待确认的移动请求 */
data class PendingDragMove(
    val uris: List<String>,
    val names: List<String>,
    val fromLeft: Boolean
)

object DragDropHolder {
    val state = MutableStateFlow(DragDropState())
    val pending = MutableStateFlow<PendingDragMove?>(null)

    fun setBounds(isLeft: Boolean, rect: Rect) {
        state.value = if (isLeft) state.value.copy(leftBounds = rect)
        else state.value.copy(rightBounds = rect)
    }

    fun start(payload: DraggedPayload, x: Float, y: Float) {
        state.value = state.value.copy(active = true, payload = payload, x = x, y = y)
    }

    fun moveTo(x: Float, y: Float) {
        val s = state.value
        if (!s.active) return
        state.value = s.copy(
            x = x,
            y = y,
            hoverLeft = s.leftBounds?.contains(androidx.compose.ui.geometry.Offset(x, y)) == true,
            hoverRight = s.rightBounds?.contains(androidx.compose.ui.geometry.Offset(x, y)) == true
        )
    }

    /** 松手：命中"对侧"面板才产生待确认的移动 */
    fun end() {
        val s = state.value
        state.value = DragDropState(leftBounds = s.leftBounds, rightBounds = s.rightBounds)
        val p = s.payload ?: return
        val targetIsLeft = s.hoverLeft
        val targetIsRight = s.hoverRight
        val droppedOnOther = if (p.fromLeft) targetIsRight else targetIsLeft
        if (!droppedOnOther) return
        pending.value = PendingDragMove(p.uris, p.names, p.fromLeft)
    }

    fun cancel() {
        state.value = DragDropState(leftBounds = state.value.leftBounds, rightBounds = state.value.rightBounds)
    }
}
