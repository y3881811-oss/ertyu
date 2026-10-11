package com.ertyu

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppListLoader
import com.ertyu.model.InstalledApp
import com.ertyu.model.InstalledAppsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==================== 已安装应用 ====================

fun MainViewModel.openInstalledApps() {
    _installedAppsState.value = InstalledAppsState(
        visible = true,
        isLoading = true
    )
    appsJob?.cancel()
    appsJob = viewModelScope.launch {
        try {
            val apps = AppListLoader.load(getApplication())
            _installedAppsState.update {
                it.copy(isLoading = false, apps = apps)
            }
        } catch (e: Exception) {
            _installedAppsState.update {
                it.copy(isLoading = false, error = e.message ?: "加载失败")
            }
        }
    }
}

fun MainViewModel.refreshInstalledApps() {
    AppListLoader.invalidate()
    openInstalledApps()
}

fun MainViewModel.closeInstalledApps() {
    appsJob?.cancel()
    appsJob = null
    _installedAppsState.value = InstalledAppsState()
}

fun MainViewModel.updateInstalledAppsFilter(q: String) {
    _installedAppsState.update { it.copy(filter = q) }
}

fun MainViewModel.toggleShowSystemApps() {
    _installedAppsState.update { it.copy(showSystem = !it.showSystem) }
}

// ==================== 启动 / 详情 / 卸载 ====================

fun MainViewModel.openApp(app: InstalledApp, context: Context) {
    try {
        val intent = context.packageManager
            .getLaunchIntentForPackage(app.packageName)
        if (intent == null) {
            _toast.value = "无法启动此应用"
            return
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "启动失败：${e.message}"
    }
}

fun MainViewModel.showAppDetails(app: InstalledApp, context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        ).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "打开详情失败：${e.message}"
    }
}

fun MainViewModel.uninstallApp(app: InstalledApp, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:${app.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "卸载失败：${e.message}"
    }
}

// ==================== 提取 APK / 分享 ====================

/**
 * 把已安装应用的 APK 复制到源文件面板当前目录。
 * 源面板由打开"已安装应用"时所在的面板决定。
 */
fun MainViewModel.extractAppApk(app: InstalledApp, sourceIsLeft: Boolean) {
    val targetDir = flowOf(sourceIsLeft).value.currentUri?.path?.let(::File)
        ?: run {
            _toast.value = "目标目录无效"
            return
        }
    val apkPath = app.apkPath
    if (apkPath.isBlank()) {
        _toast.value = "无法获取 APK 路径"
        return
    }

    viewModelScope.launch {
        try {
            val src = File(apkPath)
            if (!src.exists()) {
                _toast.value = "源 APK 不存在"
                return@launch
            }
            val outName = "${app.label}_${app.versionName}.apk"
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val dst = File(targetDir, outName)

            withContext(Dispatchers.IO) {
                src.copyTo(dst, overwrite = true)
            }

            _toast.value = "已提取：${dst.name}"
            refresh(sourceIsLeft)
        } catch (e: Exception) {
            _toast.value = "提取失败：${e.message}"
        }
    }
}

/**
 * 用系统分享面板分享 APK 文件。
 * Android 9 上从 /data/app/ 目录分享需要用 FileProvider 转 content:// URI。
 * 但 FileProvider 的 external-path 不覆盖 /data/app/，
 * 所以先把 APK 复制到应用缓存目录再分享。
 */
fun MainViewModel.shareAppApk(app: InstalledApp, context: Context) {
    val apkPath = app.apkPath
    if (apkPath.isBlank()) {
        _toast.value = "无法获取 APK 路径"
        return
    }

    viewModelScope.launch {
        try {
            val src = File(apkPath)
            if (!src.exists()) {
                _toast.value = "源 APK 不存在"
                return@launch
            }

            // 复制到 cacheDir/share/
            val shareDir = File(context.cacheDir, "share").apply { mkdirs() }
            val outName = "${app.label}_${app.versionName}.apk"
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val dst = File(shareDir, outName)

            withContext(Dispatchers.IO) {
                src.copyTo(dst, overwrite = true)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                dst
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.android.package-archive"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(
                Intent.createChooser(intent, "分享 APK").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (e: Exception) {
            _toast.value = "分享失败：${e.message}"
        }
    }
}

/**
 * 打开应用 APK 所在目录（跳到文件面板）。
 */
fun MainViewModel.openAppApkFolder(app: InstalledApp, sourceIsLeft: Boolean) {
    val apkPath = app.apkPath
    if (apkPath.isBlank()) {
        _toast.value = "无法获取 APK 路径"
        return
    }
    val parent = File(apkPath).parentFile ?: run {
        _toast.value = "无法定位目录"
        return
    }
    if (!parent.exists()) {
        _toast.value = "目录不存在"
        return
    }
    closeInstalledApps()
    openDirectory(sourceIsLeft, Uri.fromFile(parent))
}