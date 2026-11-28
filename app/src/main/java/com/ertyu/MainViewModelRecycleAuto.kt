package com.ertyu

import com.ertyu.engine.AppLogger
import com.ertyu.engine.RecycleBinManager
import com.ertyu.engine.RecyclePrefs
import com.ertyu.model.RecycleBinItem

/** 启动时按设置自动清理过期回收站条目 */
fun MainViewModel.runRecycleAutoPurge() {
    val app = getApplication<android.app.Application>()
    val days = RecyclePrefs.autoPurgeDays(app)
    if (days <= 0) return
    val items: List<RecycleBinItem> = try {
        RecycleBinManager.getItems(app)
    } catch (_: Exception) {
        return
    }
    val cutoff = System.currentTimeMillis() - days.toLong() * 24L * 3600L * 1000L
    val expired = items.filter { it.deleteTime in 1..cutoff }
    if (expired.isEmpty()) return
    expired.forEach { try { RecycleBinManager.deletePermanently(app, it) } catch (_: Exception) {} }
    AppLogger.i("Recycle", "自动清理 $days 天前的回收站条目 ${expired.size} 个")
    showToast("已自动清理 ${expired.size} 个超过 $days 天的回收站文件")
}

fun MainViewModel.setRecycleAutoPurge(days: Int) {
    RecyclePrefs.setAutoPurgeDays(getApplication(), days)
    showToast(if (days <= 0) "已关闭自动清理" else "将自动清理超过 $days 天的回收站文件")
}
