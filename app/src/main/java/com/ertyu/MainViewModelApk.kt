package com.ertyu

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ApkBrowser
import com.ertyu.engine.ApkInspector
import com.ertyu.engine.ApkInstaller
import com.ertyu.engine.SplitApkInstaller
import com.ertyu.model.ApkBrowserState
import com.ertyu.model.ApkEntry
import com.ertyu.model.ApkInfo
import com.ertyu.model.ApkInfoState
import com.ertyu.model.FileItem
import com.ertyu.model.InstallState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// ==================== APK 信息 ====================

internal fun MainViewModel.openApkInfo(item: FileItem) {
    _apkInfoState.value = ApkInfoState(
        visible = true,
        isLoading = true,
        fileName = item.name
    )
    val apkFile = item.uri.path?.let(::File)
    if (apkFile == null) {
        _apkInfoState.value = ApkInfoState(
            visible = true,
            isLoading = false,
            fileName = item.name,
            error = "无效路径"
        )
        return
    }
    viewModelScope.launch {
        val result = ApkInspector.inspect(getApplication(), apkFile)
        result.onSuccess { info ->
            _apkInfoState.update {
                it.copy(isLoading = false, info = info)
            }
        }.onFailure { e ->
            _apkInfoState.update {
                it.copy(isLoading = false, error = e.message ?: "解析失败")
            }
        }
    }
}

fun MainViewModel.closeApkInfo() {
    _apkInfoState.value = ApkInfoState()
}

fun MainViewModel.saveApkIcon(info: ApkInfo) {
    viewModelScope.launch {
        try {
            val icon = info.icon ?: run {
                _toast.value = "无图标可保存"
                return@launch
            }
            val parentDir = File(info.filePath).parentFile ?: run {
                _toast.value = "无法确定目录"
                return@launch
            }
            val safeLabel = info.label.replace(
                Regex("[\\\\/:*?\"<>|]"), "_"
            )
            val outFile = File(
                parentDir,
                "${safeLabel}_${info.packageName}.png"
            )
            FileOutputStream(outFile).use { out ->
                icon.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            _toast.value = "图标已保存：${outFile.name}"
        } catch (e: Exception) {
            _toast.value = "保存失败：${e.message}"
        }
    }
}

// ==================== APK 浏览 ====================

fun MainViewModel.openApkBrowser(isLeft: Boolean, item: FileItem) {
    val apkFile = item.uri.path?.let(::File) ?: run {
        _toast.value = "无效路径"
        return
    }
    apkBrowserSourceIsLeft = isLeft
    _apkBrowserState.value = ApkBrowserState(
        visible = true,
        apkPath = apkFile.absolutePath,
        apkName = apkFile.name,
        isLoading = true
    )
    viewModelScope.launch {
        val result = ApkBrowser.list(apkFile)
        result.onSuccess { entries ->
            _apkBrowserState.update {
                it.copy(isLoading = false, entries = entries)
            }
        }.onFailure { e ->
            _apkBrowserState.update {
                it.copy(isLoading = false, error = e.message ?: "读取失败")
            }
        }
    }
}

fun MainViewModel.closeApkBrowser() {
    _apkBrowserState.value = ApkBrowserState()
}

fun MainViewModel.updateApkBrowserFilter(q: String) {
    _apkBrowserState.update { it.copy(filterQuery = q) }
}

fun MainViewModel.extractApkEntry(entry: ApkEntry) {
    val apkPath = _apkBrowserState.value.apkPath
    val apkFile = File(apkPath)
    val targetDir = flowOf(apkBrowserSourceIsLeft)
        .value.currentUri?.path?.let(::File) ?: run {
        _toast.value = "无目标目录"
        return
    }
    viewModelScope.launch {
        val result = ApkBrowser.extractEntry(apkFile, entry.path, targetDir)
        result.onSuccess { out ->
            _toast.value = "已提取：${out.name}"
            refresh(apkBrowserSourceIsLeft)
        }.onFailure { e ->
            _toast.value = "提取失败：${e.message}"
        }
    }
}

// ==================== APK 安装 ====================

fun MainViewModel.installApk(item: FileItem) {
    val apkFile = item.uri.path?.let(::File) ?: run {
        _toast.value = "无效的 APK 路径"
        return
    }
    
    val ext = item.name.substringAfterLast('.', "").lowercase()
    if (ext == "apks" || ext == "xapk" || ext == "apkm") {
        startSplitInstall(apkFile)
    } else {
        startInstall(apkFile)
    }
}

fun MainViewModel.installApkFromInfo(info: ApkInfo) {
    startInstall(File(info.filePath))
}

private fun MainViewModel.startInstall(apkFile: File) {
    installJob?.cancel()
    _installState.value = InstallState(
        visible = true,
        fileName = apkFile.name,
        stage = "准备中…"
    )
    installJob = viewModelScope.launch {
        val result = ApkInstaller.install(
            context = getApplication(),
            apkFile = apkFile
        ) { copied, total ->
            _installState.update {
                it.copy(
                    stage = if (total > 0 && copied >= total) "提交安装…"
                    else "复制中…",
                    copied = copied,
                    total = total
                )
            }
        }

        when (result) {
            is ApkInstaller.Result.Success -> {
                _installState.update {
                    it.copy(
                        stage = "已提交，请在弹出的系统界面确认",
                        copied = it.total
                    )
                }
                delay(1200)
                _installState.value = InstallState()
            }
            is ApkInstaller.Result.NeedInstallPermission -> {
                _installState.value = InstallState()
                _pendingInstall.value = apkFile
                _toast.value = "请先允许本应用安装未知应用"
            }
            is ApkInstaller.Result.Failure -> {
                _installState.update {
                    it.copy(stage = "失败", error = result.message)
                }
            }
        }
    }
}

private fun MainViewModel.startSplitInstall(archiveFile: File) {
    installJob?.cancel()
    _installState.value = InstallState(
        visible = true,
        fileName = archiveFile.name,
        stage = "准备中…"
    )
    installJob = viewModelScope.launch {
        val result = SplitApkInstaller.install(
            context = getApplication(),
            archiveFile = archiveFile
        ) { stageMsg, cur, total ->
            _installState.update {
                it.copy(
                    stage = stageMsg,
                    copied = cur.toLong(),
                    total = total.toLong()
                )
            }
        }

        when (result) {
            is SplitApkInstaller.Result.Success -> {
                _installState.update {
                    it.copy(stage = "安装包已提交，请确认")
                }
                delay(1500)
                _installState.value = InstallState()
            }
            is SplitApkInstaller.Result.Failure -> {
                if (result.message == "NEED_PERMISSION") {
                    _installState.value = InstallState()
                    _pendingInstall.value = archiveFile
                    _toast.value = "请先允许本应用安装未知应用"
                } else {
                    _installState.update {
                        it.copy(stage = "失败", error = result.message)
                    }
                }
            }
        }
    }
}

fun MainViewModel.dismissInstallProgress() {
    installJob?.cancel()
    installJob = null
    _installState.value = InstallState()
}

fun MainViewModel.dismissInstallPermissionPrompt() {
    _pendingInstall.value = null
}