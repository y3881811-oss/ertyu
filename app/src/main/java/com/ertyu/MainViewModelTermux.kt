package com.ertyu

import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import java.io.File

fun MainViewModel.openTermuxHome(isLeft: Boolean) {
    val context = getApplication<Application>()
    val pm = context.packageManager

    // 1. 确认包名 com.termux（或 UTermux）是否已安装
    try {
        pm.getPackageInfo("com.termux", 0)
    } catch (e: PackageManager.NameNotFoundException) {
        _toast.value = "未检测到 Termux 或 UTermux 应用"
        return
    }

    // 2. 准备所有可能的路径集合
    val possiblePaths = mutableListOf<String>()

    // a. 外部存储映射路径（Android 9 下最有可能不需要特殊权限就能读的路径）
    val extRoot = Environment.getExternalStorageDirectory().absolutePath
    possiblePaths.add("$extRoot/Android/data/com.termux/files/home")
    possiblePaths.add("$extRoot/Android/data/com.termux/files")
    
    // b. 内部真实路径（由于没有 Shizuku/Root，下面这些大概率会报权限拒绝，但我们依然尝试一下）
    possiblePaths.add("/data/data/com.termux/files/home")
    possiblePaths.add("/data/user/0/com.termux/files/home")

    // 3. 遍历所有路径，尝试打开
    for (path in possiblePaths) {
        val dir = File(path)
        try {
            // 核心判断：dir.exists() 为 true 且 listFiles() 不为 null，说明不仅有这个目录，我们还有读取权限！
            if (dir.exists() && dir.listFiles() != null) {
                openDirectory(isLeft, Uri.fromFile(dir))
                _toast.value = "已打开 Termux 目录"
                return
            }
        } catch (e: SecurityException) {
            // 遇到系统权限拦截，跳过该路径
        } catch (e: Exception) {
            // 遇到其他异常，跳过
        }
    }

    // 4. 如果所有路径都失败
    _toast.value = "无法获取 Termux 目录（Android 9 通常无权限读取内部数据，若外部路径不存在则无法打开）"
}