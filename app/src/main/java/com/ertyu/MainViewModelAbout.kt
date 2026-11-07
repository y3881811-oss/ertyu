package com.ertyu

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateState(
    val isChecking: Boolean = false,
    val hasUpdate: Boolean = false,
    val latestVersion: String = "",
    val downloadUrl: String = "",
    val errorMsg: String? = null
)

val _aboutVisible = MutableStateFlow(false)
val _updateState = MutableStateFlow(UpdateState())

fun MainViewModel.openAbout() {
    _aboutVisible.value = true
}

fun MainViewModel.closeAbout() {
    _aboutVisible.value = false
    _updateState.value = UpdateState() // 退出时重置状态
}

fun MainViewModel.checkForUpdates(currentVersionName: String) {
    if (_updateState.value.isChecking) return
    _updateState.value = UpdateState(isChecking = true)

    viewModelScope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                val apiUrl = URL("https://api.github.com/repos/y3881811-oss/ertyu/releases/latest")
                val conn = apiUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "MTFM-App")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.getString("tag_name") // 例如 "v2026.11.07-1234"
                    val htmlUrl = json.getString("html_url")   // Release 页面链接
                    Pair(latestTag, htmlUrl)
                } else {
                    throw Exception("HTTP ${conn.responseCode}")
                }
            }

            val (latestTag, htmlUrl) = result
            
            // 清理版本号进行比对 (比如去掉前缀 v 和 date 里的 - 等)
            val cleanLatest = latestTag.replace("v", "").replace("-", ".")
            
            // 如果 Github 上的最新 Tag 和当前 App 的 versionName 不一致，则提示更新
            // (注意：此处用简单字符串对比，如果 CI 生成版本号变了，建议改成拆分数字对比)
            if (cleanLatest != currentVersionName) {
                _updateState.value = UpdateState(
                    isChecking = false,
                    hasUpdate = true,
                    latestVersion = latestTag,
                    downloadUrl = htmlUrl
                )
            } else {
                _updateState.value = UpdateState(isChecking = false, hasUpdate = false)
                _toast.value = "已是最新版本"
            }

        } catch (e: Exception) {
            Log.e("UpdateCheck", "检查更新失败", e)
            _updateState.value = UpdateState(isChecking = false, errorMsg = e.message)
            _toast.value = "检查更新失败：${e.message}"
        }
    }
}

fun MainViewModel.dismissUpdateDialog() {
    _updateState.value = _updateState.value.copy(hasUpdate = false)
}

fun MainViewModel.downloadUpdate(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<android.app.Application>().startActivity(intent)
    } catch (e: Exception) {
        _toast.value = "无法打开浏览器"
    }
}