package com.ertyu

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ApkParserEngine
import com.ertyu.model.FileItem
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

// 补充：如果是 Manifest 或普通文本，直接调用
fun MainViewModel.openApkInternalFile(apkPath: String, entryPath: String, entryName: String) {
    doApkInternalParse(apkPath, entryPath, entryName)
}

private fun MainViewModel.doApkInternalParse(apkPath: String, entryPath: String, entryName: String) {
    val apkFile = File(apkPath)
    viewModelScope.launch {
        _toast.value = "正在解析：$entryName"
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
    }
}