package com.ertyu.engine

import android.content.Context
import com.ertyu.model.SearchHistoryHolder

/** 搜索历史的持久化（独立 prefs） */
object SearchHistoryStore {
    private const val PREF = "eyu_search_history"
    private const val KEY = "items"
    private const val MAX = 20

    fun init(context: Context) {
        try {
            val raw = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY, "") ?: ""
            SearchHistoryHolder.items.value = raw.split('\n').filter { it.isNotBlank() }.take(MAX)
        } catch (_: Exception) {
        }
    }

    fun add(context: Context, query: String) {
        val q = query.trim()
        if (q.isEmpty()) return
        val list = (listOf(q) + SearchHistoryHolder.items.value.filter { it != q }).take(MAX)
        SearchHistoryHolder.items.value = list
        save(context, list)
    }

    fun remove(context: Context, query: String) {
        val list = SearchHistoryHolder.items.value.filter { it != query }
        SearchHistoryHolder.items.value = list
        save(context, list)
    }

    fun clear(context: Context) {
        SearchHistoryHolder.items.value = emptyList()
        save(context, emptyList())
    }

    private fun save(context: Context, list: List<String>) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putString(KEY, list.joinToString("\n")).apply()
        } catch (_: Exception) {
        }
    }
}
