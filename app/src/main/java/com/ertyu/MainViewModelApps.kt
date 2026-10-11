package com.ertyu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppListLoader
import com.ertyu.model.InstalledApp
import com.ertyu.model.InstalledAppsState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==================== 已安装应用 ====================

fun MainViewModel.openInstalledApps() {
    _installedAppsState.value = InstalledAppsState(
        visible = true,
        isLoading = true
    )
    appsJob?.cancel()
    appsJob = viewModelScope.launch {
        try {
            val apps = AppListLoader.load(getApplication())
            _installedAppsState.update {
                it.copy(isLoading = false, apps = apps)
            }
        } catch (e: Exception) {
            _installedAppsState.update {
                it.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }
}

fun MainViewModel.refreshInstalledApps() {
    AppListLoader.invalidate()
    openInstalledApps()
}

fun MainViewModel.closeInstalledApps() {
    appsJob?.cancel()
    appsJob = null
    _installedAppsState.value = InstalledAppsState()
}

fun MainViewModel.updateInstalledAppsFilter(q: String) {
    _installedAppsState.update { it.copy(filter = q) }
}

fun MainViewModel.toggleShowSystemApps() {
    _installedAppsState.update { it.copy(showSystem = !it.showSystem) }
}

fun MainViewModel.openApp(app: InstalledApp, context: Context) {
    try {
        val intent = context.packageManager
            .getLaunchIntentForPackage(app.packageName)
        if (intent == null) {
            _toast.value = "无法启动此应用"
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "启动失败：${e.message}"
    }
}

fun MainViewModel.showAppDetails(app: InstalledApp, context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "打开详情失败：${e.message}"
    }
}

fun MainViewModel.uninstallApp(app: InstalledApp, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "卸载失败：${e.message}"
    }
}