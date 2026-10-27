package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.SettingsStore
import com.ertyu.engine.ShizukuFileEngine
import com.ertyu.engine.DirectFileEngine
import com.ertyu.model.PanelMode
import com.ertyu.model.SortMode
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun MainViewModel.loadSettings() {
    val s = SettingsStore.load(getApplication())
    _settingsState.value = s
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

fun MainViewModel.toggleGridMode() {
    _settingsState.update { it.copy(isGridMode = !it.isGridMode) }
    persistSettings()
}

fun MainViewModel.setUseRecycleBin(use: Boolean) {
    _settingsState.update { it.copy(useRecycleBin = use) }
    persistSettings()
}

fun MainViewModel.setWebDavSharePath(path: String) {
    _settingsState.update { it.copy(webDavSharePath = path) }
    persistSettings()
}

fun MainViewModel.setWebDavPort(port: Int) {
    _settingsState.update { it.copy(webDavPort = port) }
    persistSettings()
}

fun MainViewModel.setWebDavUsername(u: String) {
    _settingsState.update { it.copy(webDavUsername = u) }
    persistSettings()
}

fun MainViewModel.setWebDavPassword(p: String) {
    _settingsState.update { it.copy(webDavPassword = p) }
    persistSettings()
}

fun MainViewModel.addCustomPath(path: String) {
    if (path.isBlank()) return
    val current = _settingsState.value.customPaths
    if (current.contains(path)) {
        _toast.value = "路径已存在"
        return
    }
    _settingsState.update { it.copy(customPaths = current + path) }
    persistSettings()
}

fun MainViewModel.removeCustomPath(path: String) {
    val current = _settingsState.value.customPaths
    _settingsState.update { it.copy(customPaths = current - path) }
    persistSettings()
}

fun MainViewModel.switchEngine(useShizuku: Boolean) {
    engine = if (useShizuku) ShizukuFileEngine() else DirectFileEngine()
    _toast.value = if (useShizuku) "已切换到 Shizuku 引擎" else "已切换到普通引擎"
    refresh(true)
    refresh(false)
}

private fun MainViewModel.persistSettings() {
    viewModelScope.launch {
        SettingsStore.save(getApplication(), _settingsState.value)
    }
}

fun MainViewModel.openSettings() { _settingsVisible.value = true }
fun MainViewModel.closeSettings() { _settingsVisible.value = false }
fun MainViewModel.openHelp() { _helpVisible.value = true }
fun MainViewModel.closeHelp() { _helpVisible.value = false }
fun MainViewModel.showToast(msg: String) { _toast.value = msg }