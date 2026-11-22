package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.SpaceAnalyzerEngine
import com.ertyu.model.BigFileEntry
import com.ertyu.model.FolderUsage
import com.ertyu.model.SpaceAnalyzerHolder
import com.ertyu.model.SpaceAnalyzerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private object SpaceAnalyzerJobs {
    var job: Job? = null
}

fun MainViewModel.openSpaceAnalyzer(path: String? = null) {
    val start = path ?: leftPanel.value.currentUri?.path ?: "/storage/emulated/0"
    SpaceAnalyzerHolder.state.update { it.copy(visible = true, root = start, error = null) }
}

fun MainViewModel.closeSpaceAnalyzer() {
    SpaceAnalyzerJobs.job?.cancel()
    SpaceAnalyzerJobs.job = null
    SpaceAnalyzerHolder.state.value = SpaceAnalyzerState()
}

fun MainViewModel.setSpaceAnalyzerRoot(path: String) {
    SpaceAnalyzerHolder.state.update { it.copy(root = path) }
}

fun MainViewModel.startSpaceScan(minSizeMb: Int = SpaceAnalyzerHolder.state.value.minSizeMb) {
    val snap = SpaceAnalyzerHolder.state.value
    val root = File(snap.root)
    if (!root.exists() || !root.isDirectory) {
        showToast("目录不存在：${snap.root}")
        return
    }
    SpaceAnalyzerJobs.job?.cancel()
    SpaceAnalyzerHolder.state.update {
        it.copy(
            isScanning = true,
            finished = false,
            error = null,
            minSizeMb = minSizeMb,
            scannedFiles = 0,
            totalBytes = 0L,
            bigFiles = emptyList(),
            topFolders = emptyList()
        )
    }
    val minBytes = minSizeMb.toLong() * 1024L * 1024L
    SpaceAnalyzerJobs.job = viewModelScope.launch {
        try {
            val r = SpaceAnalyzerEngine.scan(root, minBytes) { files, bytes ->
                SpaceAnalyzerHolder.state.update { it.copy(scannedFiles = files, totalBytes = bytes) }
            }
            SpaceAnalyzerHolder.state.update {
                it.copy(
                    isScanning = false,
                    finished = true,
                    scannedFiles = r.totalFiles,
                    totalBytes = r.totalBytes,
                    bigFiles = r.bigFiles.map { b -> BigFileEntry(b.path, File(b.path).name, b.size) },
                    topFolders = r.folders.map { f -> FolderUsage(f.path, f.bytes, f.fileCount) }
                )
            }
            AppLogger.i("SpaceAnalyzer", "扫描完成：${r.totalFiles} 个文件，${ItemSizeEngine.format(r.totalBytes)}")
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.e("SpaceAnalyzer", "扫描失败", e)
            SpaceAnalyzerHolder.state.update { it.copy(isScanning = false, error = e.message ?: "扫描失败") }
        } finally {
            SpaceAnalyzerJobs.job = null
        }
    }
}

fun MainViewModel.stopSpaceScan() {
    SpaceAnalyzerJobs.job?.cancel()
    SpaceAnalyzerJobs.job = null
    SpaceAnalyzerHolder.state.update { it.copy(isScanning = false) }
}

/** 在大文件列表里点击后：关闭分析页并跳到它所在目录 */
fun MainViewModel.revealAmongFiles(path: String) {
    val parent = File(path).parentFile
    if (parent == null) {
        showToast("找不到所在目录")
        return
    }
    closeSpaceAnalyzer()
    openDirectory(true, Uri.fromFile(parent))
    showToast("已定位到 ${parent.name}")
}
