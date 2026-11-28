package com.ertyu.model

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 「隐藏指定类型」过滤：按扩展名把列表里的文件藏起来（文件夹永远显示）。
 * 状态放在全局单例，双面板共用一份；持久化见 engine/HiddenExtPrefs。
 */
object HiddenExtHolder {
    /** 小写、不带点的扩展名集合 */
    val exts = MutableStateFlow<Set<String>>(emptySet())
    val enabled = MutableStateFlow(true)
    val visible = MutableStateFlow(false)

    fun normalize(raw: String): String =
        raw.trim().lowercase().removePrefix(".").removePrefix("*.")

    fun isHidden(fileName: String): Boolean {
        if (!enabled.value) return false
        val set = exts.value
        if (set.isEmpty()) return false
        if (!fileName.contains('.')) return false
        val ext = normalize(fileName.substringAfterLast('.'))
        return ext.isNotEmpty() && ext in set
    }
}
