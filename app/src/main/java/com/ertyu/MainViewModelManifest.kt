package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.AxmlParser
import com.ertyu.model.FileItem
import com.ertyu.model.ManifestHolder
import com.ertyu.model.ManifestState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.openManifestViewer(item: FileItem) {
    val apk = item.uri.path?.let(::File)
    if (apk == null || !apk.isFile) {
        showToast("无效的 APK 路径")
        return
    }
    ManifestHolder.state.value = ManifestState(visible = true, loading = true, apkName = item.name)
    viewModelScope.launch {
        val result = withContext(Dispatchers.IO) { AxmlParser.parseApk(apk) }
        if (result == null) {
            AppLogger.e("Manifest", "解析失败: ${apk.absolutePath}", null)
            ManifestHolder.state.update {
                it.copy(loading = false, error = "解析失败：文件可能不是标准二进制 AndroidManifest.xml")
            }
        } else {
            AppLogger.i("Manifest", "解析 ${item.name}：包名 ${result.packageName}，权限 ${result.permissions.size} 个")
            ManifestHolder.state.update {
                it.copy(
                    loading = false,
                    xml = result.xml,
                    permissions = result.permissions,
                    packageName = result.packageName
                )
            }
        }
    }
}

fun MainViewModel.closeManifestViewer() {
    ManifestHolder.state.value = ManifestState()
}
