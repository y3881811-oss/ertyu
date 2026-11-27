package com.ertyu.engine

import android.content.Context
import android.content.SharedPreferences

/**
 * 崩溃循环保护：同一分钟内连续两次「启动后没活到健康点」就判定为崩溃循环，
 * 提示用户进入安全模式（关掉重负载功能）。
 * 只用 Context + SharedPreferences，所有读写都吞异常，绝不能反过来把启动搞崩。
 */
object CrashGuard {
    private const val PREF = "eyu_crash_guard"
    private const val K_COUNT = "count"
    private const val K_LAST = "last_start"
    private const val K_HEALTHY = "healthy"
    private const val K_PROMPTED = "prompted_at_count"

    private const val WINDOW_MS = 120_000L
    private const val THRESHOLD = 2

    private fun prefs(context: Context): SharedPreferences? = try {
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    } catch (_: Exception) {
        null
    }

    fun onAppStart(context: Context) {
        try {
            val sp = prefs(context) ?: return
            val now = System.currentTimeMillis()
            val last = sp.getLong(K_LAST, 0L)
            var count = sp.getInt(K_COUNT, 0)
            val healthy = sp.getBoolean(K_HEALTHY, false)

            if (last > 0 && now - last in 1..WINDOW_MS && !healthy) {
                count++
            } else if (now - last > WINDOW_MS) {
                count = 0
            }
            sp.edit()
                .putInt(K_COUNT, count)
                .putLong(K_LAST, now)
                .putBoolean(K_HEALTHY, false)
                .apply()
        } catch (_: Exception) {
        }
    }

    /** 运行满一段时间没崩，就认为这次启动是健康的，计数清零 */
    fun markHealthy(context: Context) {
        try {
            prefs(context)?.edit()?.putBoolean(K_HEALTHY, true)?.putInt(K_COUNT, 0)?.apply()
        } catch (_: Exception) {
        }
    }

    fun crashCount(context: Context): Int = try {
        prefs(context)?.getInt(K_COUNT, 0) ?: 0
    } catch (_: Exception) {
        0
    }

    fun isSafeMode(context: Context): Boolean = crashCount(context) >= THRESHOLD

    fun shouldPrompt(context: Context): Boolean {
        if (!isSafeMode(context)) return false
        return try {
            val sp = prefs(context) ?: return false
            sp.getInt(K_PROMPTED, -1) != crashCount(context)
        } catch (_: Exception) {
            false
        }
    }

    fun markPrompted(context: Context) {
        try {
            prefs(context)?.edit()?.putInt(K_PROMPTED, crashCount(context))?.apply()
        } catch (_: Exception) {
        }
    }

    fun reset(context: Context) {
        try {
            prefs(context)?.edit()?.clear()?.apply()
        } catch (_: Exception) {
        }
    }
}
