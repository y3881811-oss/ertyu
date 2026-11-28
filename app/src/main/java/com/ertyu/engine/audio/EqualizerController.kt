package com.ertyu.engine.audio

import android.content.Context
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow

data class EqPreset(val name: String, val gains: FloatArray) {
    override fun equals(other: Any?) = this === other
    override fun hashCode() = name.hashCode()
}

data class EqualizerState(
    val supported: Boolean = true,
    val enabled: Boolean = false,
    val presetIndex: Int = 0,
    val error: String? = null
)

/**
 * 均衡器：挂在播放 Service 里那个 ExoPlayer 的 audio session 上。
 * 设备不支持时只记录 error，不影响播放。
 */
object EqualizerController {

    private const val PREF = "eyu_eq"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PRESET = "preset"

    val presets = listOf(
        EqPreset("原声", floatArrayOf(0f, 0f, 0f, 0f, 0f)),
        EqPreset("流行", floatArrayOf(0.20f, 0.10f, 0f, 0.10f, 0.20f)),
        EqPreset("摇滚", floatArrayOf(0.40f, 0.20f, -0.10f, 0.20f, 0.30f)),
        EqPreset("古典", floatArrayOf(0.30f, 0.15f, -0.10f, 0.15f, 0.25f)),
        EqPreset("爵士", floatArrayOf(0.25f, 0.10f, 0.05f, 0.10f, 0.20f)),
        EqPreset("低音增强", floatArrayOf(0.60f, 0.40f, 0.10f, 0f, 0f))
    )

    val state = MutableStateFlow(EqualizerState())

    private var equalizer: Equalizer? = null
    private var sessionId: Int = 0
    private var appContext: Context? = null

    fun bind(context: Context) {
        appContext = context.applicationContext
        val sp = try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        } catch (_: Exception) {
            null
        }
        val enabled = sp?.getBoolean(KEY_ENABLED, false) ?: false
        val preset = sp?.getInt(KEY_PRESET, 0) ?: 0
        state.value = state.value.copy(enabled = enabled, presetIndex = preset)
        if (sessionId != 0) applyInternal()
    }

    /** Service 创建 player 后调用；audioSessionId 变化时也要再调 */
    fun attach(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        appContext = context.applicationContext
        if (sessionId == audioSessionId && equalizer != null) return
        release()
        sessionId = audioSessionId
        try {
            equalizer = Equalizer(0, audioSessionId).apply { enabled = false }
            state.value = state.value.copy(supported = true, error = null)
            applyInternal()
        } catch (e: Exception) {
            equalizer = null
            state.value = state.value.copy(
                supported = false,
                error = "此设备不支持均衡器（${e.javaClass.simpleName}）"
            )
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
        persist(context, enabled, state.value.presetIndex)
        applyInternal()
    }

    fun setPreset(context: Context, index: Int) {
        if (index !in presets.indices) return
        state.value = state.value.copy(presetIndex = index, enabled = true)
        persist(context, true, index)
        applyInternal()
    }

    private fun applyInternal() {
        val eq = equalizer ?: return
        try {
            val st = state.value
            if (!st.enabled) {
                eq.enabled = false
                return
            }
            val range = eq.bandLevelRange
            val min = range[0].toInt()
            val max = range[1].toInt()
            val gains = presets.getOrElse(st.presetIndex) { presets[0] }.gains
            val bands = eq.numberOfBands.toInt()
            for (b in 0 until bands) {
                val frac = if (b < gains.size) gains[b] else 0f
                // 正增益往 max 走，负增益往 min 走
                val raw = if (frac >= 0f) max * frac else min * -frac
                val level = raw.toInt().coerceIn(min, max)
                eq.setBandLevel(b.toShort(), level.toShort())
            }
            eq.enabled = true
        } catch (_: Exception) {
        }
    }

    private fun persist(context: Context, enabled: Boolean, presetIndex: Int) {
        try {
            context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).putInt(KEY_PRESET, presetIndex).apply()
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            equalizer?.enabled = false
            equalizer?.release()
        } catch (_: Exception) {
        }
        equalizer = null
        sessionId = 0
    }
}
