package com.ertyu

import android.app.Application
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow

// 用于通知 UI 层拉起 SAF 选择器的状态
val _termuxSafRequestState = MutableStateFlow(false)

fun MainViewModel.openTermuxHome(isLeft: Boolean) {
    val context = getApplication<Application>()
    val pm = context.packageManager

    // 确认包名是否存在
    try {
        pm.getPackageInfo("com.termux", 0)
    } catch (e: PackageManager.NameNotFoundException) {
        _toast.value = "未检测到包名为 com.termux 的应用（UTermux）"
        return
    }

    // 不再硬编码尝试 File 路径，直接请求 UI 层拉起 SAF 授权
    _termuxSafRequestState.value = true
}

fun MainViewModel.dismissTermuxSafRequest() {
    _termuxSafRequestState.value = false
}