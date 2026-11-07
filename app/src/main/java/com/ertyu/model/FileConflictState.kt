package com.ertyu.model

enum class ConflictAction { OVERWRITE, SKIP, KEEP_BOTH, CANCEL }

data class FileConflictState(
    val visible: Boolean = false,
    val conflictingNames: List<String> = emptyList(),
    val onResolve: ((ConflictAction) -> Unit)? = null
)