package com.ertyu.model

enum class NumberPosition(val label: String) {
    PREFIX("前缀"),
    SUFFIX("后缀")
}

data class RenamePreview(
    val oldName: String,
    val newName: String
)

data class BatchRenameState(
    val visible: Boolean = false,
    val sourceIsLeft: Boolean = true,
    val targets: List<FileItem> = emptyList(),
    val prefix: String = "",
    val suffix: String = "",
    val findText: String = "",
    val replaceText: String = "",
    val useRegex: Boolean = false,
    val useNumbering: Boolean = false,
    val numberStart: Int = 1,
    val numberStep: Int = 1,
    val numberDigits: Int = 2,
    val numberPosition: NumberPosition = NumberPosition.SUFFIX,
    val previews: List<RenamePreview> = emptyList(),
    val isExecuting: Boolean = false,
    val error: String? = null
) {
    val canExecute: Boolean
        get() = previews.isNotEmpty() &&
                !isExecuting &&
                previews.any { it.newName != it.oldName } &&
                previews.none { it.newName.isEmpty() }
}