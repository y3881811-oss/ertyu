package com.ertyu

import com.ertyu.model.BatchRenameState
import com.ertyu.model.ChecksumState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 批量重命名 / 校验和 的全局状态。
 * 这样 MainViewModel.kt 不需要改动。
 */
object BatchToolsState {
    val batchRenameState = MutableStateFlow(BatchRenameState())
    val checksumState = MutableStateFlow(ChecksumState())
}