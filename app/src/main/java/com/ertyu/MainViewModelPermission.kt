package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.PermissionManager
import com.ertyu.model.FileItem
import com.ertyu.model.PermissionHolder
import com.ertyu.model.PermissionTarget
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openPermissionDialog(item: FileItem) {
    val mode = _settingsState.value.engineType
    if (mode != 2) {
        showToast(
            if (mode == 1) "Shizuku 通道暂不支持改权限，请在设置里切到 Root"
            else "改权限需要 Root，请先在设置 → 引擎里切到 Root"
        )
        return
    }
    if (item.path.startsWith("/storage/emulated") || item.path.startsWith("/sdcard")) {
        showToast("/sdcard 是 FUSE 文件系统，chmod 不会生效（只对 /data、/system 有效）")
        return
    }
    PermissionHolder.target.value = PermissionTarget(item.path, item.name, item.isDirectory)
    PermissionHolder.currentMode.value = ""
    viewModelScope.launch {
        val cur = PermissionManager.readMode(item.path, useRoot = true)
        if (cur != null) PermissionHolder.currentMode.value = cur
    }
}

fun MainViewModel.closePermissionDialog() {
    PermissionHolder.target.value = null
    PermissionHolder.currentMode.value = ""
}

fun MainViewModel.applyPermission(mode: String) {
    val t = PermissionHolder.target.value ?: return
    if (!PermissionManager.isValidMode(mode)) {
        showToast("权限值必须是 3~4 位八进制，例如 644、755")
        return
    }
    viewModelScope.launch {
        val r = PermissionManager.apply(t.path, mode, useRoot = true)
        r.onSuccess {
            AppLogger.i("Permission", "chmod $mode ${t.path}")
            showToast("已设置为 $mode")
            PermissionHolder.target.value = null
            refresh(true)
            refresh(false)
        }.onFailure { e ->
            AppLogger.e("Permission", "chmod 失败 ${t.path}", e)
            showToast("修改失败：${e.message}")
        }
    }
}
