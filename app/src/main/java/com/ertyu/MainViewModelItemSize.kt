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
 * 结果写进 ItemSizeHolder（key = 绝对路径），列表行自己订阅显示。
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
                    val key = item.path
                    val hit = ItemSizeEngine.cached(key)
                    if (hit != null) {
                        ItemSizeHolder.sizes.update { it + (key to hit.bytes) }
                        if (hit.truncated) ItemSizeHolder.truncated.update { it + key }
                        return@withPermit
                    }
                    ItemSizeHolder.computing.update { it + key }
                    try {
                        val r = ItemSizeEngine.calculate(File(key))
                        ItemSizeHolder.sizes.update { it + (key to r.bytes) }
                        if (r.truncated) ItemSizeHolder.truncated.update { it + key }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogger.d("ItemSize", "计算失败 $key: ${e.message}")
                    } finally {
                        ItemSizeHolder.computing.update { it - key }
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

/**
 * 某个路径内容发生变化（删除/移动/复制/重命名/解压…）后调用：
 * 让缓存和界面上与它相关的条目（自身、子目录、祖先目录）全部失效。
 */
fun MainViewModel.invalidateSizeFor(path: String) {
    if (path.isBlank()) return
    ItemSizeEngine.invalidateUnder(path)
    ItemSizeHolder.sizes.update { m -> m.filterKeys { !ItemSizeEngine.related(it, path) } }
    ItemSizeHolder.truncated.update { s -> s.filterNot { ItemSizeEngine.related(it, path) }.toSet() }
    ItemSizeHolder.computing.update { s -> s.filterNot { ItemSizeEngine.related(it, path) }.toSet() }
}
