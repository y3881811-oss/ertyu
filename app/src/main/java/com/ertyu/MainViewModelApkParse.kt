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
        _parseProgressState.value = ParseProgressState(
            visible = true, 
            title = "正在解析", 
            message = entryName
        )
        
        try {
            val result = when {
                entryName == "AndroidManifest.xml" -> ApkParserEngine.getManifest(apkFile)
                entryName == "resources.arsc" -> ApkParserEngine.getResourcesArsc(apkFile)
                else -> ApkParserEngine.getInternalFileContent(apkFile, entryPath)
            }

            result.onSuccess { text ->
                val cacheFileName = if (entryName == "resources.arsc") "resources.html" else entryName
                val cacheFile = File(getApplication<Application>().cacheDir, cacheFileName)
                
                withContext(Dispatchers.IO) {
                    cacheFile.writeText(text)
                }
                
                // 👇 修复：加上 uriString
                val fileUri = android.net.Uri.fromFile(cacheFile)
                val item = FileItem(
                    name = cacheFileName,
                    uriString = fileUri.toString(), // <--- 加在这里
                    uri = fileUri,
                    isDirectory = false,
                    path = cacheFile.absolutePath
                )
                
                try {
                    openFile(item)
                } catch (e: Exception) {
                    _toast.value = "打开文件失败: ${e.message}"
                }
                
            }.onFailure { e ->
                _toast.value = "解析失败：${e.message ?: e.javaClass.simpleName}"
            }
        } catch (e: Exception) {
            _toast.value = "发生未知异常：${e.message}"
        } finally {
            _parseProgressState.value = ParseProgressState()
        }
    }
}