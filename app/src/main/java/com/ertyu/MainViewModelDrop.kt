package com.ertyu

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 接收从其他应用拖进来的文件（照片、文档等），直接复制到目标面板当前目录。
 */
fun MainViewModel.importDroppedUris(uris: List<Uri>, targetIsLeft: Boolean) {
    if (uris.isEmpty()) return
    val panel = flowOf(targetIsLeft).value
    val targetUri = panel.currentUri ?: run {
        showToast("目标面板还没有打开目录")
        return
    }
    val app = getApplication<Application>()

    viewModelScope.launch {
        var ok = 0
        var fail = 0
        for (uri in uris) {
            val name = displayName(app, uri)
            AppLogger.i("Drop", "接收拖入文件：$name ($uri)")
            val success = withContext(Dispatchers.IO) {
                engine.copy(uri, targetUri) { }
            }
            if (success) ok++ else fail++
        }
        refresh(targetIsLeft)
        showToast(if (fail == 0) "已复制 $ok 个文件" else "完成：成功 $ok，失败 $fail")
    }
}

private fun displayName(app: Application, uri: Uri): String {
    return try {
        app.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) return c.getString(idx) ?: uri.lastPathSegment ?: "dropped"
            }
        }
        uri.lastPathSegment ?: "dropped"
    } catch (_: Exception) {
        uri.lastPathSegment ?: "dropped"
    }
}
