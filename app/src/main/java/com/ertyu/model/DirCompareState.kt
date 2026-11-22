package com.ertyu.model

import com.ertyu.engine.DiffKind
import com.ertyu.engine.DirDiffEntry
import kotlinx.coroutines.flow.MutableStateFlow

data class DirCompareState(
    val visible: Boolean = false,
    val leftRoot: String = "",
    val rightRoot: String = "",
    val entries: List<DirDiffEntry> = emptyList(),
    val isScanning: Boolean = false,
    val isSyncing: Boolean = false,
    val progressText: String = "",
    val deep: Boolean = true,
    val onlyDiff: Boolean = true,
    val selected: Set<String> = emptySet(),
    val error: String? = null
) {
    val leftOnly: Int get() = entries.count { it.kind == DiffKind.LEFT_ONLY }
    val rightOnly: Int get() = entries.count { it.kind == DiffKind.RIGHT_ONLY }
    val different: Int get() = entries.count { it.kind == DiffKind.DIFFERENT }
    val visibleEntries: List<DirDiffEntry>
        get() = if (onlyDiff) entries.filter { it.kind != DiffKind.SAME } else entries
}

object DirCompareHolder {
    val state = MutableStateFlow(DirCompareState())
}
