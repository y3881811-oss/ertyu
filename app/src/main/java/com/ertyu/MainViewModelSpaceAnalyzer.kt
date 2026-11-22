package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.RecycleBinManager
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
    SpaceAnalyzerHolder.state.update { it.copy(root = path, selectedPaths = emptySet()) }
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
            topFolders = emptyList(),
            selectedPaths = emptySet()
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

// ==================== 大文件多选 / 批量删除 ====================

fun MainViewModel.toggleBigFileSelection(path: String) {
    SpaceAnalyzerHolder.state.update {
        val set = it.selectedPaths.toMutableSet()
        if (!set.add(path)) set.remove(path)
        it.copy(selectedPaths = set)
    }
}

fun MainViewModel.selectAllBigFiles() {
    SpaceAnalyzerHolder.state.update { it.copy(selectedPaths = it.bigFiles.map { f -> f.path }.toSet()) }
}

fun MainViewModel.clearBigFileSelection() {
    SpaceAnalyzerHolder.state.update { it.copy(selectedPaths = emptySet()) }
}

/**
 * 批量删除选中的大文件。
 * 设置里开了回收站就移入回收站（可恢复），否则直接删除。
 */
fun MainViewModel.deleteSelectedBigFiles() {
    val snap = SpaceAnalyzerHolder.state.value
    val paths = snap.selectedPaths.toList()
    if (paths.isEmpty() || snap.deleting) return
    val useTrash = _settingsState.value.useRecycleBin

    viewModelScope.launch {
        SpaceAnalyzerHolder.state.update { it.copy(deleting = true) }
        var okCount = 0
        var trashCount = 0
        var freed = 0L
        for (p in paths) {
            val f = File(p)
            if (!f.exists()) continue
            val size = try { f.length() } catch (_: Exception) { 0L }
            val success = try {
                if (useTrash) RecycleBinManager.moveToTrash(getApplication(), f, false, size) else f.delete()
            } catch (e: Exception) {
                AppLogger.e("SpaceAnalyzer", "删除失败 $p", e)
                false
            }
            if (success) {
                okCount++
                freed += size
                if (useTrash) trashCount++
            }
            // 单个文件变化：让相关目录大小缓存失效
            invalidateSizeFor(f.parent ?: p)
        }

        val removed = paths.toSet()
        SpaceAnalyzerHolder.state.update {
            it.copy(
                deleting = false,
                selectedPaths = emptySet(),
                bigFiles = it.bigFiles.filterNot { b -> b.path in removed },
                totalBytes = (it.totalBytes - freed).coerceAtLeast(0L)
            )
        }
        val sizeText = ItemSizeEngine.format(freed)
        showToast(
            if (useTrash) "已移入回收站 $trashCount 项，释放 $sizeText"
            else "已彻底删除 $okCount 项，释放 $sizeText"
        )
        AppLogger.i("SpaceAnalyzer", "批量删除 $okCount 项，释放 $sizeText（回收站=$useTrash）")
    }
}
