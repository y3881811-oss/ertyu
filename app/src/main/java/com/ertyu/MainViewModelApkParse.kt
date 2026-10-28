package com.ertyu

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ApkParserEngine
import com.ertyu.model.FileItem
import com.ertyu.model.ParseProgressState
import com.ertyu.model.PendingParseState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.requestApkInternalParse(apkPath: String, entryPath: String, entryName: String) {
    _pendingParseState.value = PendingParseState(
        visible = true,
        apkPath = apkPath,
        entryPath = entryPath,
        entryName = entryName
    )
}

fun MainViewModel.cancelApkInternalParse() {
    _pendingParseState.value = PendingParseState()
}

fun MainViewModel.confirmApkInternalParse() {
    val state = _pendingParseState.value
    if (!state.visible) return
    val apkPath = state.apkPath
    val entryPath = state.entryPath
    val entryName = state.entryName
    
    _pendingParseState.value = PendingParseState()
    doApkInternalParse(apkPath, entryPath, entryName)
}

fun MainViewModel.openApkInternalFile(apkPath: String, entryPath: String, entryName: String) {
    doApkInternalParse(apkPath, entryPath, entryName)
}

private fun MainViewModel.doApkInternalParse(apkPath: String, entryPath: String, entryName: String) {
    val apkFile = File(apkPath)
    viewModelScope.launch {
        // 显示解析进度条
        _parseProgressState.value = ParseProgressState(visible = true, title = "正在解析", message = entryName)
        try {
            val result = when {
                entryName == "AndroidManifest.xml" -> ApkParserEngine.getManifest(apkFile)
                entryName == "resources.arsc" -> ApkParserEngine.getResourcesArsc(apkFile)
                else -> ApkParserEngine.getInternalFileContent(apkFile, entryPath)
            }

            result.onSuccess { text ->
                val cacheFile = File(getApplication<Application>().cacheDir, entryName)
                withContext(Dispatchers.IO) {
                    cacheFile.writeText(text)
                }
                val item = FileItem(
                    name = entryName,
                    uri = android.net.Uri.fromFile(cacheFile),
                    isDirectory = false,
                    path = cacheFile.absolutePath
                )
                openFile(item)
            }.onFailure { e ->
                _toast.value = "解析失败：${e.message}"
            }
        } finally {
            // 无论成功失败，关闭进度条
            _parseProgressState.value = ParseProgressState()
        }
    }
}