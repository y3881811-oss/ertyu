package com.ertyu.engine

import android.content.Context

/** 记住每个媒体文件上次播放到的位置（毫秒） */
object PlaybackProgressStore {
    private const val PREF = "eyu_playback"
    private const val MIN_SAVE_MS = 5_000L

    private fun key(path: String) = "p_" + Integer.toHexString(path.hashCode())

    fun save(context: Context, path: String, positionMs: Long, durationMs: Long) {
        if (path.isBlank()) return
        try {
            val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            if (positionMs < MIN_SAVE_MS || durationMs <= 0 || positionMs > durationMs - 10_000L) {
                sp.edit().remove(key(path)).apply()
            } else {
                sp.edit().putLong(key(path), positionMs).apply()
            }
        } catch (_: Exception) {
        }
    }

    fun load(context: Context, path: String): Long = try {
        if (path.isBlank()) 0L
        else context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getLong(key(path), 0L)
    } catch (_: Exception) {
        0L
    }

    fun clear(context: Context, path: String) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(key(path)).apply()
        } catch (_: Exception) {
        }
    }
}
