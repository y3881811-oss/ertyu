package com.ertyu.engine

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow

/** 列表是否按类型分组显示（独立 prefs） */
object GroupPrefs {
    private const val PREF = "eyu_group"
    private const val KEY = "group_enabled"

    val enabled = MutableStateFlow(false)

    fun init(context: Context) {
        try {
            enabled.value = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY, false)
        } catch (_: Exception) {
        }
    }

    fun setEnabled(context: Context, value: Boolean) {
        enabled.value = value
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY, value).apply()
        } catch (_: Exception) {
        }
    }
}
