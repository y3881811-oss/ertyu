package com.ertyu.engine

sealed class MoveResult {
    data object Success : MoveResult()

    /** 复制阶段失败，源文件未受影响 */
    data object CopyFailed : MoveResult()

    /** 复制成功且校验通过，但源文件删除失败；目标文件已完整存在 */
    data class DeleteFailed(val copiedTargetPath: String) : MoveResult()
}