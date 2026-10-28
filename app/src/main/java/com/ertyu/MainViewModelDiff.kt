package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.TextDiffer
import com.ertyu.model.DiffLineType
import com.ertyu.model.DiffState
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** 支持的文本扩展名 */
private val DIFF_TEXT_EXTS = setOf(
    "txt", "md", "json", "xml", "kt", "java", "kts",
    "gradle", "properties", "yml", "yaml", "html", "css", "js",
    "log", "ini", "conf", "cfg", "sh", "bat", "csv", "tsv",
    "smali", "c", "cpp", "h", "hpp", "py", "rb", "go", "rs",
    "php", "swift", "ts", "tsx", "jsx", "vue"
)

/**
 * 从当前多选面板启动文件对比。
 * 要求恰好选中 2 个文本文件。
 */
fun MainViewModel.startFileDiff(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    val targets = state.items.filter {
        state.selectedUris.contains(it.uriString)
    }
    if (targets.size != 2) {
        _toast.value = "请恰好选中 2 个文件"
        return
    }
    val a = targets[0]
    val b = targets[1]
    val aOk = isDiffTextFile(a)
    val bOk = isDiffTextFile(b)
    if (!aOk || !bOk) {
        _toast.value = "仅支持文本文件对比"
        return
    }
    openDiff(a, b)
}

private fun isDiffTextFile(item: FileItem): Boolean {
    if (item.isDirectory) return false
    val ext = item.name.substringAfterLast('.', "").lowercase()
    return ext in DIFF_TEXT_EXTS
}

fun MainViewModel.openDiff(a: FileItem, b: FileItem) {
    DiffStateHolder.state.value = DiffState(
        visible = true,
        leftPath = a.uri.path ?: "",
        rightPath = b.uri.path ?: "",
        leftName = a.name,
        rightName = b.name,
        isLoading = true
    )
    viewModelScope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                val leftFile = File(a.uri.path ?: "")
                val rightFile = File(b.uri.path ?: "")
                if (!leftFile.exists() || !rightFile.exists()) {
                    throw IllegalArgumentException("文件不存在")
                }
                val leftText = leftFile.readText(Charsets.UTF_8)
                val rightText = rightFile.readText(Charsets.UTF_8)
                TextDiffer.diff(leftText, rightText)
            }

            var added = 0
            var removed = 0
            var modified = 0
            result.forEach {
                when (it.type) {
                    DiffLineType.ADDED -> added++
                    DiffLineType.REMOVED -> removed++
                    DiffLineType.MODIFIED -> modified++
                    else -> {}
                }
            }

            DiffStateHolder.state.value = DiffStateHolder.state.value.copy(
                isLoading = false,
                lines = result,
                addedCount = added,
                removedCount = removed,
                modifiedCount = modified
            )
        } catch (e: Exception) {
            DiffStateHolder.state.value = DiffStateHolder.state.value.copy(
                isLoading = false,
                error = e.message ?: "读取失败"
            )
        }
    }
}

fun MainViewModel.closeDiff() {
    DiffStateHolder.state.value = DiffState()
}