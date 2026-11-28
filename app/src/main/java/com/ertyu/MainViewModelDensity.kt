package com.ertyu

import com.ertyu.engine.AppLogger
import com.ertyu.engine.SettingsStore
import kotlinx.coroutines.flow.update

/** 列表密度：0 紧凑 / 1 标准 / 2 宽松 */
fun MainViewModel.setListDensity(value: Int) {
    _settingsState.update { it.copy(listDensity = value.coerceIn(0, 2)) }
    SettingsStore.save(getApplication(), _settingsState.value)
    AppLogger.i("Settings", "列表密度 -> $value")
}
