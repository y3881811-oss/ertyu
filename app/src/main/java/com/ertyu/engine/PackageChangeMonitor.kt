package com.ertyu.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build

/**
 * 监听应用安装 / 卸载 / 更新广播，自动失效 AppListLoader 缓存。
 *
 * 使用方式：
 *   onCreate: PackageChangeMonitor.start(context)
 *   onDestroy: PackageChangeMonitor.stop(context)
 *
 * 收到广播后只是清空缓存，下次打开"已安装应用"时会重新扫描。
 */
object PackageChangeMonitor {

    private var receiver: BroadcastReceiver? = null
    private var registered = false

    fun start(context: Context) {
        if (registered) return

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }

        val r = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                // 应用列表变化，立即失效缓存
                AppListLoader.invalidate()
            }
        }
        receiver = r

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    r, filter, Context.RECEIVER_NOT_EXPORTED
                )
            } else {
                context.registerReceiver(r, filter)
            }
            registered = true
        } catch (_: Exception) {
            registered = false
            receiver = null
        }
    }

    fun stop(context: Context) {
        val r = receiver ?: return
        runCatching { context.unregisterReceiver(r) }
        receiver = null
        registered = false
    }
}