package com.ertyu

import android.app.Application
import com.ertyu.engine.AppLogger
import com.ertyu.engine.CrashGuard
import com.ertyu.engine.CrashHandler
import com.ertyu.engine.HiddenExtPrefs
import com.ertyu.engine.SearchHistoryStore
import com.ertyu.engine.ItemSizePrefs
import com.ertyu.engine.ThumbnailDiskCache
import com.ertyu.engine.UpdateCacheCleaner
import com.ertyu.engine.LogcatCollector

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 1. 初始化日志系统
        AppLogger.init(this)
        // 读取“显示文件夹大小”开关
        ItemSizePrefs.init(this)
        // 隐藏文件类型设置
        HiddenExtPrefs.init(this)
        // 搜索历史
        SearchHistoryStore.init(this)
        
        // 2. 初始化全局崩溃捕获（写入崩溃日志）
        CrashHandler.init(this)
        
        // 3. 👇 启动底层 Logcat 监听器，将系统报错混写进 app_xxx.log
        // 清理过期的更新包缓存（保留 24 小时内）
        UpdateCacheCleaner.clean(this)
        ThumbnailDiskCache.init(this)

        LogcatCollector.start()
        // 崩溃循环保护：记录本次启动（连续崩溃会提示安全模式）
        CrashGuard.onAppStart(this)
    }
}