package com.ertyu.model

import com.ertyu.engine.VaultEntry
import kotlinx.coroutines.flow.MutableStateFlow

data class VaultState(
    val visible: Boolean = false,
    val hasPassword: Boolean = false,
    val unlocked: Boolean = false,
    val entries: List<VaultEntry> = emptyList(),
    val busy: Boolean = false,
    val busyText: String = "",
    val error: String? = null
)

object VaultHolder {
    val state = MutableStateFlow(VaultState())
    /** 仅保存在内存里，退出页面即清空 */
    var password: String = ""
    /** 解锁后自动移入的待处理项 */
    var pendingItems: List<com.ertyu.model.FileItem> = emptyList()
}
