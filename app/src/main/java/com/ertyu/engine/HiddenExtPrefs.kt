package com.ertyu.engine

import android.content.Context
import com.ertyu.model.HiddenExtHolder

/** 隐藏扩展名设置的持久化（独立 prefs，避免动 SettingsStore） */
object HiddenExtPrefs {
    private const val PREF = "eyu_hidden_ext"
    private const val KEY_EXTS = "exts"
    private const val KEY_ENABLED = "enabled"

    fun init(context: Context) {
        try {
            val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val raw = sp.getString(KEY_EXTS, "") ?: ""
            HiddenExtHolder.exts.value = raw.split(',')
                .map { HiddenExtHolder.normalize(it) }
                .filter { it.isNotEmpty() }
                .toSet()
            HiddenExtHolder.enabled.value = sp.getBoolean(KEY_ENABLED, true)
        } catch (_: Exception) {
        }
    }

    fun save(context: Context) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_EXTS, HiddenExtHolder.exts.value.joinToString(","))
                .putBoolean(KEY_ENABLED, HiddenExtHolder.enabled.value)
                .apply()
        } catch (_: Exception) {
        }
    }
}
