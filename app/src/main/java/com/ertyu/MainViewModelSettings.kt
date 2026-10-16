package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.SettingsStore
import com.ertyu.model.AppSettings
import com.ertyu.model.PanelMode
import com.ertyu.model.SortMode
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==================== 设置加载/保存 ====================

fun MainViewModel.loadSettings() {
    val s = SettingsStore.load(getApplication())
    _settingsState.value = s
    // 应用到当前面板
    _leftPanel.update { it.copy(sortMode = s.sortModeDefault) }
    _rightPanel.update { it.copy(sortMode = s.sortModeDefault) }
}

fun MainViewModel.setPanelMode(mode: PanelMode) {
    _settingsState.update { it.copy(panelMode = mode) }
    persistSettings()
}

fun MainViewModel.setShowHiddenDefault(show: Boolean) {
    _settingsState.update { it.copy(showHiddenByDefault = show) }
    persistSettings()
}

fun MainViewModel.setSortModeDefault(sort: SortMode) {
    _settingsState.update { it.copy(sortModeDefault = sort) }
    persistSettings()
}

private fun MainViewModel.persistSettings() {
    viewModelScope.launch {
        SettingsStore.save(getApplication(), _settingsState.value)
    }
}

// ==================== 设置页开关 ====================

fun MainViewModel.openSettings() {
    _settingsVisible.value = true
}

fun MainViewModel.closeSettings() {
    _settingsVisible.value = false
}

fun MainViewModel.openHelp() {
    _helpVisible.value = true
}

fun MainViewModel.closeHelp() {
    _helpVisible.value = false
}