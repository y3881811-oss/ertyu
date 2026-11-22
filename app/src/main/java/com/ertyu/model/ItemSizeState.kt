package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 文件夹真实大小的运行时状态（双面板共用一份）。
 * 不放进 MainViewModel，避免改动已有核心类。
 */
object ItemSizeHolder {
    /** uriString -> 字节数 */
    val sizes = MutableStateFlow<Map<String, Long>>(emptyMap())
    /** 正在计算的 uriString，用于显示“计算中” */
    val computing = MutableStateFlow<Set<String>>(emptySet())
    /** 超过扫描上限被截断的 uriString，显示时加 “+” */
    val truncated = MutableStateFlow<Set<String>>(emptySet())
    /** 是否在文件列表里显示文件夹大小（持久化在 ItemSizePrefs） */
    val enabled = MutableStateFlow(true)

    fun clearAll() {
        sizes.value = emptyMap()
        computing.value = emptySet()
        truncated.value = emptySet()
    }
}
