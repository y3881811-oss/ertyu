package com.ertyu.engine.audio

import android.content.Context

/** 记住上次使用的播放倍速 */
object AudioSpeedPrefs {
    private const val PREF = "eyu_audio"
    private const val KEY_SPEED = "speed"
    val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    fun get(context: Context): Float =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE).getFloat(KEY_SPEED, 1.0f)

    fun set(context: Context, value: Float) {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_SPEED, value).apply()
    }

    fun indexOf(value: Float): Int = speeds.indexOfFirst { kotlin.math.abs(it - value) < 0.01f }
        .let { if (it < 0) 1 else it }
}
