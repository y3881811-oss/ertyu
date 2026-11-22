package com.ertyu.engine

import android.content.Context
import android.os.Environment
import com.ertyu.model.RecycleBinItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object RecycleBinManager {
    private const val PREF_NAME = "mtfm_recycle_meta"
    private const val KEY_META = "meta"
    val trashDir = File(Environment.getExternalStorageDirectory(), ".MTFM_Trash")

    fun getItems(context: Context): List<RecycleBinItem> {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = sp.getString(KEY_META, "[]") ?: "[]"
        val result = mutableListOf<RecycleBinItem>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    RecycleBinItem(
                        id = obj.getString("id"),
                        originalPath = obj.getString("originalPath"),
                        name = obj.getString("name"),
                        isDirectory = obj.getBoolean("isDirectory"),
                        deleteTime = obj.getLong("deleteTime"),
                        size = obj.optLong("size", 0L)
                    )
                )
            }
        } catch (e: Exception) {}
        return result.sortedByDescending { it.deleteTime }
    }

    private fun saveItems(context: Context, items: List<RecycleBinItem>) {
        val arr = JSONArray()
        items.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("originalPath", it.originalPath)
            obj.put("name", it.name)
            obj.put("isDirectory", it.isDirectory)
            obj.put("deleteTime", it.deleteTime)
            obj.put("size", it.size)
            arr.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_META, arr.toString()).apply()
    }

    fun moveToTrash(context: Context, file: File, isDir: Boolean, size: Long): Boolean {
        if (!trashDir.exists()) trashDir.mkdirs()
        val id = UUID.randomUUID().toString()
        val targetFile = File(trashDir, id)
        
        val success = if (file.renameTo(targetFile)) {
            true
        } else {
            // 跨分区移动
            try {
                if (isDir) file.copyRecursively(targetFile, true) else file.copyTo(targetFile, true)
                file.deleteRecursively()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (success) {
            val items = getItems(context).toMutableList()
            items.add(0, RecycleBinItem(id, file.absolutePath, file.name, isDir, System.currentTimeMillis(), size))
            saveItems(context, items)
        }
        return success
    }

    fun restore(context: Context, item: RecycleBinItem): Boolean {
        val src = File(trashDir, item.id)
        if (!src.exists()) return false
        val dest = File(item.originalPath)
        dest.parentFile?.mkdirs()
        
        val success = if (src.renameTo(dest)) true else {
            try {
                if (item.isDirectory) src.copyRecursively(dest, true) else src.copyTo(dest, true)
                src.deleteRecursively()
                true
            } catch (e: Exception) { false }
        }
        
        if (success) {
            val items = getItems(context).filter { it.id != item.id }
            saveItems(context, items)
        }
        return success
    }

    fun deletePermanently(context: Context, item: RecycleBinItem) {
        val src = File(trashDir, item.id)
        src.deleteRecursively()
        val items = getItems(context).filter { it.id != item.id }
        saveItems(context, items)
    }

    fun clearAll(context: Context) {
        trashDir.deleteRecursively()
        saveItems(context, emptyList())
    }

    /**
     * 还原但不覆盖原文件：若原位置已有同名文件，自动改名为「原名 (恢复)」。
     * 返回 (是否成功, 提示文案)。
     */
    fun restoreSafely(context: Context, item: RecycleBinItem): Pair<Boolean, String?> {
        val src = File(trashDir, item.id)
        if (!src.exists()) return false to "文件已丢失：${item.name}"

        var dest = File(item.originalPath)
        var renamed = false
        if (dest.exists()) {
            val parent = dest.parentFile ?: File(item.originalPath).parentFile
            val isDir = item.isDirectory
            val base = if (isDir || !item.name.contains('.')) item.name else item.name.substringBeforeLast('.')
            val ext = if (isDir || !item.name.contains('.')) "" else "." + item.name.substringAfterLast('.')
            var i = 1
            while (i < 1000) {
                val candidate = File(parent, if (i == 1) "$base (恢复)$ext" else "$base (恢复 $i)$ext")
                if (!candidate.exists()) {
                    dest = candidate
                    renamed = true
                    break
                }
                i++
            }
            if (!renamed) {
                dest = File(parent, "$base (恢复 ${System.currentTimeMillis()})$ext")
                renamed = true
            }
        }

        dest.parentFile?.mkdirs()
        val success = if (src.renameTo(dest)) {
            true
        } else {
            try {
                if (item.isDirectory) src.copyRecursively(dest, true) else src.copyTo(dest, true)
                src.deleteRecursively()
                true
            } catch (e: Exception) {
                false
            }
        }

        if (success) {
            saveItems(context, getItems(context).filter { it.id != item.id })
        }
        val msg = when {
            !success -> "还原失败：${item.name}"
            renamed -> "已还原为「${dest.name}」（原位置已有同名文件）"
            else -> "已还原：${item.name}"
        }
        return success to msg
    }

    /** 清理早于 days 天前放入的条目；days <= 0 表示全部清空。返回清理条数。 */
    fun purgeOlderThan(context: Context, days: Int): Int {
        val cutoff = if (days <= 0) Long.MAX_VALUE
        else System.currentTimeMillis() - days.toLong() * 24L * 3600L * 1000L
        val items = getItems(context)
        var removed = 0
        val keep = ArrayList<RecycleBinItem>()
        for (it in items) {
            if (it.deleteTime < cutoff) {
                try {
                    File(trashDir, it.id).deleteRecursively()
                } catch (_: Exception) { }
                removed++
            } else {
                keep.add(it)
            }
        }
        if (removed > 0) saveItems(context, keep)
        return removed
    }
}