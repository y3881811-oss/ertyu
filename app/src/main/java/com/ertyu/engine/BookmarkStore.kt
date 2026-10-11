package com.ertyu.engine

import android.content.Context
import android.net.Uri
import com.ertyu.model.Bookmark
import com.ertyu.model.RecentItem
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BookmarkStore {

    private const val PREF_NAME = "mtfm_bookmarks"
    private const val KEY_BOOKMARKS = "bookmarks"
    private const val KEY_RECENTS = "recents"
    private const val MAX_RECENTS = 30

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun loadBookmarks(context: Context): List<Bookmark> {
        val raw = prefs(context).getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val path = o.optString("path")
                val name = o.optString("name")
                val uri = o.optString("uri")
                if (path.isEmpty() || uri.isEmpty()) null
                else Bookmark(path = path, name = name, uriString = uri)
            }
        }.getOrDefault(emptyList())
    }

    fun saveBookmarks(context: Context, list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach {
            val o = JSONObject()
            o.put("path", it.path)
            o.put("name", it.name)
            o.put("uri", it.uriString)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun loadRecents(context: Context): List<RecentItem> {
        val raw = prefs(context).getString(KEY_RECENTS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val path = o.optString("path")
                val name = o.optString("name")
                val uri = o.optString("uri")
                val last = o.optLong("last", 0L)
                if (path.isEmpty() || uri.isEmpty()) null
                else RecentItem(
                    path = path,
                    name = name,
                    uriString = uri,
                    lastAccess = last
                )
            }
        }.getOrDefault(emptyList())
    }

    fun addRecent(context: Context, dir: File) {
        val uri = Uri.fromFile(dir).toString()
        val list = loadRecents(context).toMutableList()
        list.removeAll { it.uriString == uri }
        list.add(
            0,
            RecentItem(
                path = dir.absolutePath,
                name = dir.name.ifBlank { dir.absolutePath },
                uriString = uri,
                lastAccess = System.currentTimeMillis()
            )
        )
        val trimmed = list.take(MAX_RECENTS)
        val arr = JSONArray()
        trimmed.forEach {
            val o = JSONObject()
            o.put("path", it.path)
            o.put("name", it.name)
            o.put("uri", it.uriString)
            o.put("last", it.lastAccess)
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_RECENTS, arr.toString()).apply()
    }

    fun clearRecents(context: Context) {
        prefs(context).edit().remove(KEY_RECENTS).apply()
    }
}