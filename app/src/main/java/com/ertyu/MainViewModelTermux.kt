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
        _toast.value = "未检测到包名为 com.termux 的应用"
        return
    }

    val extRoot = Environment.getExternalStorageDirectory().absolutePath
    
    // 你要测试的所有路径
    val possiblePaths = listOf(
        "$extRoot/Android/data/com.termux/files/home",
        "$extRoot/Android/data/com.termux/files",
        "/data/data/com.termux/files/home",
        "/data/user/0/com.termux/files/home"
    )

    val debugReport = StringBuilder()
    debugReport.append("探测结果：\n\n")

    for (path in possiblePaths) {
        val dir = File(path)
        debugReport.append("路径: $path\n")
        debugReport.append("存在: ${dir.exists()}\n")
        try {
            val files = dir.listFiles()
            if (files != null) {
                debugReport.append("读取: 成功 (包含 ${files.size} 个文件/文件夹)\n")
                // 如果真的能读，直接打开
                openDirectory(isLeft, Uri.fromFile(dir))
                _toast.value = "已成功打开！"
                return
            } else {
                debugReport.append("读取: 失败 (listFiles返回null，可能是系统拦截)\n")
            }
        } catch (e: SecurityException) {
            debugReport.append("读取: 被系统拒绝 (SecurityException)\n")
        } catch (e: Exception) {
            debugReport.append("读取: 异常 (${e.message})\n")
        }
        debugReport.append("\n")
    }

    // 如果所有路径都失败，弹出一个系统级 AlertDialog 展示报告
    // 这里为了不引入复杂的 UI 状态，直接用 Toast 打印长文本（建议用弹窗看，Toast 可能显示不全）
    // 如果你有 _alertDialogState 之类的状态，请把它显示在弹窗里
    // 这里为了简单，我们打 Logcat 和 Toast 长文本
    android.util.Log.e("TermuxDebug", debugReport.toString())
    
    // 为了让你能直观看到，通过 MainViewModel 的 toast 发送
    // (如果你有弹窗控制逻辑，请换掉 _toast.value 用弹窗显示)
    _toast.value = debugReport.toString() 
}