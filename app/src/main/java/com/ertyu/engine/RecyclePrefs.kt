package com.ertyu.engine

import android.content.Context

/** 回收站自动清理设置：0 表示关闭 */
object RecyclePrefs {
    private const val PREF = "eyu_recycle"
    private const val KEY_DAYS = "auto_purge_days"

    fun autoPurgeDays(context: Context): Int = try {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getInt(KEY_DAYS, 0)
    } catch (_: Exception) {
        0
    }

    fun setAutoPurgeDays(context: Context, days: Int) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putInt(KEY_DAYS, days).apply()
        } catch (_: Exception) {
        }
    }
}
