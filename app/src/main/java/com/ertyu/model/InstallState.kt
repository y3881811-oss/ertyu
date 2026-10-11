package com.ertyu.model

data class InstallState(
    val visible: Boolean = false,
    val fileName: String = "",
    val stage: String = "",
    val copied: Long = 0,
    val total: Long = 0,
    val error: String? = null
) {
    val percent: Int
        get() = if (total <= 0) 0
        else ((copied * 100) / total).toInt().coerceIn(0, 100)
}