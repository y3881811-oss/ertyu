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
                // 1. 处理缓存文件名：如果是 arsc，解析出来的是 HTML 文本，存为 .html 方便预览
                val cacheFileName = if (entryName == "resources.arsc") "resources.html" else entryName
                val cacheFile = File(getApplication<Application>().cacheDir, cacheFileName)
                
                // 2. 写入缓存文件
                withContext(Dispatchers.IO) {
                    cacheFile.writeText(text)
                }
                
                // 3. 构建 FileItem
                val item = FileItem(
                    name = cacheFileName,
                    uri = android.net.Uri.fromFile(cacheFile),
                    isDirectory = false,
                    path = cacheFile.absolutePath
                )
                
                // 4. 尝试打开，并捕获可能发生的异常
                try {
                    openFile(item)
                } catch (e: Exception) {
                    _toast.value = "打开文件失败: ${e.message}"
                }
                
            }.onFailure { e ->
                // 如果解析失败，一定要提示出来，不能无声无息
                _toast.value = "解析失败：${e.message ?: e.javaClass.simpleName}"
            }
        } catch (e: Exception) {
            // 兜底捕获
            _toast.value = "发生未知异常：${e.message}"
        } finally {
            // 无论成功失败，都要关闭进度条
            _parseProgressState.value = ParseProgressState()
        }
    }
}