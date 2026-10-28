package com.ertyu.model

enum class DiffLineType {
    SAME,
    ADDED,
    REMOVED,
    MODIFIED
}

data class DiffLine(
    val leftLineNo: Int?,        // 左文件行号（null 表示此行不存在）
    val rightLineNo: Int?,       // 右文件行号
    val leftText: String,        // 左文件内容
    val rightText: String,       // 右文件内容
    val type: DiffLineType
)

data class DiffState(
    val visible: Boolean = false,
    val leftPath: String = "",
    val rightPath: String = "",
    val leftName: String = "",
    val rightName: String = "",
    val isLoading: Boolean = false,
    val lines: List<DiffLine> = emptyList(),
    val addedCount: Int = 0,
    val removedCount: Int = 0,
    val modifiedCount: Int = 0,
    val error: String? = null
)