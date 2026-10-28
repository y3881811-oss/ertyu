package com.ertyu

import android.app.Application
import com.ertyu.engine.CrashHandler

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 初始化全局崩溃捕获
        CrashHandler.init(this)
    }
}