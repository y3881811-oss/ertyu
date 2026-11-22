package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.model.FileItem
import com.ertyu.model.ItemSizeHolder
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File

private object ItemSizeJobs {
    var job: Job? = null
}

/**
 * 扫描当前目录里所有子文件夹的真实大小。
 * 结果写进 ItemSizeHolder，列表行自己订阅显示。
 */
fun MainViewModel.startVisibleFolderSizeScan(items: List<FileItem>) {
    ItemSizeJobs.job?.cancel()
    ItemSizeJobs.job = null
    if (!ItemSizeHolder.enabled.value) return

    val dirs = items.filter {
        it.isDirectory && it.path.isNotBlank() && !ItemSizeEngine.shouldSkip(it.path)
    }
    if (dirs.isEmpty()) return

    val semaphore = Semaphore(2)
    ItemSizeJobs.job = viewModelScope.launch {
        for (item in dirs) {
            launch {
                semaphore.withPermit {
                    val hit = ItemSizeEngine.cached(item.path)
                    if (hit != null) {
                        ItemSizeHolder.sizes.update { it + (item.uriString to hit.bytes) }
                        if (hit.truncated) ItemSizeHolder.truncated.update { it + item.uriString }
                        return@withPermit
                    }
                    ItemSizeHolder.computing.update { it + item.uriString }
                    try {
                        val r = ItemSizeEngine.calculate(File(item.path))
                        ItemSizeHolder.sizes.update { it + (item.uriString to r.bytes) }
                        if (r.truncated) ItemSizeHolder.truncated.update { it + item.uriString }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.d("ItemSize", "计算失败 ${item.path}: ${e.message}")
                    } finally {
                        ItemSizeHolder.computing.update { it - item.uriString }
                    }
                }
            }
        }
    }
}

fun MainViewModel.cancelFolderSizeScan() {
    ItemSizeJobs.job?.cancel()
    ItemSizeJobs.job = null
    ItemSizeHolder.computing.value = emptySet()
}
