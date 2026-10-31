package com.ertyu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    // ===== 原有逻辑保持不变 =====

    // ===== 新增：设置状态，必须公开给 SettingsAdvanced =====
    private val _settingsState = MutableStateFlow(AppSettings())
    val settingsState: StateFlow<AppSettings> = _settingsState.asStateFlow()

    // ===== 新增：切换引擎入口 =====
    fun switchEngine(index: Int) {
        if (_settingsState.value.engineType == index) return

        // 先更新设置中的 engineType
        _settingsState.value = _settingsState.value.copy(engineType = index)

        // 异步重建 FileEngine
        viewModelScope.launch {
            rebuildEngine(index)
        }
    }

    private suspend fun rebuildEngine(index: Int) {
        // TODO: 根据 index (0=SAF, 1=Shizuku, 2=Root) 重建 FileEngine
        // 并刷新左右面板当前目录列表
    }

    // 用于 SettingsAdvanced 中的 Toast 提示
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}