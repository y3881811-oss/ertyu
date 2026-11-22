package com.ertyu.engine

import android.content.Context
import com.ertyu.model.ItemSizeHolder

/** 独立的 SharedPreferences，避免改动 SettingsStore */
object ItemSizePrefs {
    private const val PREF = "eyu_item_size"
    private const val KEY_ENABLED = "show_folder_size"

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        ItemSizeHolder.enabled.value = sp.getBoolean(KEY_ENABLED, true)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        ItemSizeHolder.enabled.value = enabled
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
        if (!enabled) ItemSizeHolder.clearAll()
    }
}
