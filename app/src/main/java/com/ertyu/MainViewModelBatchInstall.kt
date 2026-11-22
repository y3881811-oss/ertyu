package com.ertyu

import com.ertyu.model.BatchInstallHolder
import com.ertyu.model.BatchInstallState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.update

/**
 * 批量安装 APK：系统每次安装都要用户确认，所以做成“逐个提交”的队列，
 * 不会一次性弹出一堆安装器。
 */
fun MainViewModel.openBatchInstall(items: List<FileItem>) {
    val apks = items.filter {
        !it.isDirectory && (it.name.endsWith(".apk", ignoreCase = true) ||
            it.name.endsWith(".xapk", ignoreCase = true) ||
            it.name.endsWith(".apks", ignoreCase = true) ||
            it.name.endsWith(".apkm", ignoreCase = true))
    }
    if (apks.isEmpty()) {
        showToast("选中的文件里没有可安装的 APK")
        return
    }
    BatchInstallHolder.state.value = BatchInstallState(visible = true, items = apks)
}

fun MainViewModel.closeBatchInstall() {
    BatchInstallHolder.state.value = BatchInstallState()
}

fun MainViewModel.installNextInQueue() {
    val snap = BatchInstallHolder.state.value
    if (snap.finished) {
        showToast("队列已全部提交安装")
        return
    }
    val item = snap.items[snap.index]
    installApk(item)
    BatchInstallHolder.state.update { it.copy(index = it.index + 1) }
}

fun MainViewModel.resetBatchInstall() {
    BatchInstallHolder.state.update { it.copy(index = 0) }
}
