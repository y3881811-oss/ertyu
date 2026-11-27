package com.ertyu

import com.ertyu.engine.CrashGuard
import com.ertyu.engine.ItemSizePrefs
import com.ertyu.model.SafeModeHolder
import com.ertyu.model.SafeModeState

/** 启动时检查是否需要提示「安全模式」 */
fun MainViewModel.checkSafeModeOnLaunch() {
    val app = getApplication<android.app.Application>()
    if (!CrashGuard.shouldPrompt(app)) return
    CrashGuard.markPrompted(app)
    SafeModeHolder.state.value = SafeModeState(visible = true, crashCount = CrashGuard.crashCount(app))
}

fun MainViewModel.dismissSafeModeDialog() {
    SafeModeHolder.state.value = SafeModeState()
}

/** 一键降级：关掉最耗资源的功能，让 App 先能稳定用起来 */
fun MainViewModel.applySafeModeDowngrade() {
    val app = getApplication<android.app.Application>()
    ItemSizePrefs.setEnabled(app, false)
    SafeModeHolder.state.value = SafeModeState()
    showToast("已关闭文件夹大小扫描；可以随时在「空间分析」里重新打开")
}

/** 正常退出安全模式（清空崩溃计数） */
fun MainViewModel.exitSafeMode() {
    val app = getApplication<android.app.Application>()
    CrashGuard.reset(app)
    SafeModeHolder.state.value = SafeModeState()
    showToast("已退出安全模式")
}
