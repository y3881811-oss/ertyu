package com.ertyu.engine

import android.content.Context

/**
 * 更新提示的本地偏好：记录用户「跳过此版本」的 tag。
 * 同一 tag 在 7 天内不再提示；出现更新的版本或过了 7 天会重新提示。
 */
object UpdatePrefs {
    private const val PREF = "eyu_update"
    private const val KEY_TAG = "skip_tag"
    private const val KEY_TIME = "skip_at"
    private const val SKIP_TTL_MS = 7L * 24 * 60 * 60 * 1000

    fun skip(context: Context, tag: String) {
        if (tag.isBlank()) return
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun shouldSkip(context: Context, tag: String): Boolean {
        val sp = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val saved = sp.getString(KEY_TAG, null) ?: return false
        if (saved != tag) return false
        val at = sp.getLong(KEY_TIME, 0L)
        return System.currentTimeMillis() - at < SKIP_TTL_MS
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
