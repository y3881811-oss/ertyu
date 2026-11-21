package com.ertyu

import android.app.Application
import com.ertyu.engine.AppLogger
import com.ertyu.engine.CrashHandler

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化日志系统（必须在最前面，以捕获后续可能的崩溃）
        AppLogger.init(this)
        AppLogger.i("App", "EYU文件管理器启动，版本: ${packageManager.getPackageInfo(packageName, 0).versionName}")
        
        // 2. 初始化全局崩溃捕获
        CrashHandler.init(this)
    }
}