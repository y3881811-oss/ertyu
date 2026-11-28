package com.ertyu

import android.app.Application
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ArchiveBrowser
import com.ertyu.model.ArchiveEntry
import com.ertyu.model.ImageViewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif", "avif")

fun ArchiveEntry.isPreviewableImage(): Boolean =
    !isDirectory && name.substringAfterLast('.', "").lowercase() in IMAGE_EXT

/** 直接把压缩包里的图片解到缓存并打开图片查看器，无需先解压到目录 */
fun MainViewModel.previewArchiveEntry(entry: ArchiveEntry) {
    if (!entry.isPreviewableImage()) {
        showToast("该文件不是图片")
        return
    }
    val state = ArchiveViewerStateHolder.state.value
    val archiveFile = File(state.archivePath)
    if (!archiveFile.isFile) {
        showToast("压缩包已不可用")
        return
    }
    val dir = File(getApplication<Application>().cacheDir, "archive_preview").apply { mkdirs() }
    val safeName = entry.name.replace('/', '_').ifBlank { "preview_${entry.path.hashCode()}" }

    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            ArchiveBrowser.extractEntry(archiveFile, entry.path, safeName, dir)
        }
        result.onSuccess { out ->
            AppLogger.i("ArchiveViewer", "预览图片: ${entry.path}")
            _imageViewer.value = ImageViewerState(
                isOpen = true,
                uri = Uri.fromFile(out),
                fileName = entry.name
            )
        }.onFailure { e ->
            AppLogger.e("ArchiveViewer", "预览失败: ${entry.path}", e)
            showToast("预览失败：${e.message ?: "未知错误"}")
        }
    }
}
