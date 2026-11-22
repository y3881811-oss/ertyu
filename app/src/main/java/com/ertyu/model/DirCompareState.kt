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

    /**
     * 真正会被同步的条目。
     * toRight=true 表示 左 -> 右：只能同步「左侧有」的项，右侧独有的没有来源，必须排除。
     */
    fun targets(toRight: Boolean): List<DirDiffEntry> {
        val pool = entries.filter { it.kind != DiffKind.SAME }
        val chosen = if (selected.isEmpty()) pool else pool.filter { it.relPath in selected }
        return chosen.filter { if (toRight) it.kind != DiffKind.RIGHT_ONLY else it.kind != DiffKind.LEFT_ONLY }
    }

    /** 选择里"这个方向没法同步"的条目数（用于给用户解释） */
    fun skipped(toRight: Boolean): Int {
        val pool = entries.filter { it.kind != DiffKind.SAME }
        val chosen = if (selected.isEmpty()) 0 else pool.count { it.relPath in selected }
        return chosen - targets(toRight).size
    }

    /** 目标侧已存在、会被覆盖的项数 */
    fun overwriteCount(toRight: Boolean): Int = targets(toRight).count { it.kind == DiffKind.DIFFERENT }

    /** 同步来源总大小 */
    fun sourceBytes(toRight: Boolean): Long =
        targets(toRight).sumOf { if (toRight) it.leftSize else it.rightSize }
}

object DirCompareHolder {
    val state = MutableStateFlow(DirCompareState())
}
