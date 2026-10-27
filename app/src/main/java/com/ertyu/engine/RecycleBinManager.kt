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
}