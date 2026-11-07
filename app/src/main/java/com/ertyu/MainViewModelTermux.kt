package com.ertyu

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import java.io.File

fun MainViewModel.openTermuxHome(isLeft: Boolean) {
    val context = getApplication<Application>()
    val pm = context.packageManager

    try {
        pm.getPackageInfo("com.termux", 0)
    } catch (e: PackageManager.NameNotFoundException) {
        _toast.value = "未检测到包名为 com.termux 的应用（UTermux）"
        return
    }

    val extRoot = Environment.getExternalStorageDirectory().absolutePath
    
    val possiblePaths = listOf(
        "$extRoot/Android/data/com.termux/files/home",
        "$extRoot/Android/data/com.termux/files",
        "/data/data/com.termux/files/home",
        "/data/user/0/com.termux/files/home"
    )

    var success = false
    for (path in possiblePaths) {
        val dir = File(path)
        if (dir.exists() && dir.listFiles() != null) {
            openDirectory(isLeft, Uri.fromFile(dir))
            _toast.value = "已打开 Termux 目录"
            success = true
            break
        }
    }

    if (!success) {
        // 👇 发现无法访问时，弹出友好的提示，告诉用户怎么解决
        _termuxGuidanceVisible.value = true
    }
}

// 增加一个控制提示弹窗的状态
val _termuxGuidanceVisible = kotlinx.coroutines.flow.MutableStateFlow(false)

fun MainViewModel.dismissTermuxGuidance() {
    _termuxGuidanceVisible.value = false
}

fun MainViewModel.copyTermuxCommand() {
    val command = "ln -s /data/data/com.termux/files/home /storage/emulated/0/TermuxHome"
    val clipboard = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Termux Command", command)
    clipboard.setPrimaryClip(clip)
    _toast.value = "命令已复制，请去 UTermux 中粘贴执行"
    dismissTermuxGuidance()
}