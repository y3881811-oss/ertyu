package com.ertyu

import com.ertyu.engine.AppLogger
import kotlinx.coroutines.flow.update

/** 外观设置：主题模式（0 跟随系统 / 1 浅色 / 2 深色）+ 强调色索引 */
fun MainViewModel.setThemeMode(mode: Int) {
    _settingsState.update { it.copy(themeMode = mode.coerceIn(0, 2)) }
    persistSettings()
    AppLogger.i("Settings", "主题模式 -> $mode")
}

fun MainViewModel.setAccentColor(index: Int) {
    _settingsState.update { it.copy(accentColor = index.coerceAtLeast(0)) }
    persistSettings()
    AppLogger.i("Settings", "强调色 -> $index")
}
