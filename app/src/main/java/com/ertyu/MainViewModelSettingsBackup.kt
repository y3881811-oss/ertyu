package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.ItemSizePrefs
import com.ertyu.engine.RootFileEngine
import com.ertyu.engine.SettingsBackup
import com.ertyu.engine.SettingsStore
import com.ertyu.engine.ShizukuFileEngine
import com.ertyu.model.SettingsBackupHolder
import com.ertyu.model.SettingsBackupState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun MainViewModel.exportSettingsBackup() {
    val state = SettingsBackupHolder.state.value
    if (state.busy) return
    SettingsBackupHolder.state.update { it.copy(busy = true) }
    viewModelScope.launch {
        val file = SettingsBackup.writeToDownload(
            _settingsState.value,
            com.ertyu.model.ItemSizeHolder.enabled.value
        )
        SettingsBackupHolder.state.update { it.copy(busy = false) }
        if (file != null) {
            showToast("已备份到 ${file.absolutePath}")
            AppLogger.i("SettingsBackup", "导出设置：${file.absolutePath}")
        } else {
            showToast("备份失败，请检查存储权限")
        }
    }
}

/** 选中文件后先解析，交给界面做二次确认 */
fun MainViewModel.prepareSettingsImport(uri: Uri) {
    SettingsBackupHolder.state.update { it.copy(busy = true) }
    viewModelScope.launch {
        val text = SettingsBackup.readUri(getApplication(), uri)
        val payload = text?.let { SettingsBackup.parse(it) }
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: "备份文件"
        SettingsBackupHolder.state.value = SettingsBackupState(pending = payload, fileName = name)
        if (payload == null) {
            showToast("不是有效的设置备份文件")
        }
    }
}

fun MainViewModel.cancelSettingsImport() {
    SettingsBackupHolder.state.value = SettingsBackupState()
}

fun MainViewModel.applySettingsImport() {
    val pending = SettingsBackupHolder.state.value.pending ?: return
    val s = pending.settings
    _settingsState.value = s
    SettingsStore.save(getApplication(), s)
    engine = when (s.engineType) {
        1 -> ShizukuFileEngine()
        2 -> RootFileEngine()
        else -> DirectFileEngine()
    }
    _leftPanel.update { it.copy(sortMode = s.sortModeDefault) }
    _rightPanel.update { it.copy(sortMode = s.sortModeDefault) }
    ItemSizePrefs.setEnabled(getApplication(), pending.showFolderSize)
    SettingsBackupHolder.state.value = SettingsBackupState()
    showToast("设置已恢复")
    AppLogger.i("SettingsBackup", "恢复设置：面板=${s.panelMode} 引擎=${s.engineType} 回收站=${s.useRecycleBin}")
    refresh(true)
    refresh(false)
}
