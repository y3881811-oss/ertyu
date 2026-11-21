package com.ertyu

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
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
    val htmlUrl: String = "",
    val downloadUrl: String = "",
    val errorMsg: String? = null
)

val downloadProxies = listOf(
    "" to "直连 GitHub (需科学上网或网络较好)",
    "https://ghfast.top/" to "高速节点 1 (韩国/日本等 CDN)",
    "https://gh-proxy.org/" to "高速节点 2 (美国/香港 CDN)",
    "https://ghproxy.net/" to "高速节点 3 (法国 CDN)",
    "https://wget.la/" to "高速节点 4 (多地区 CDN)"
)

val _aboutVisible = MutableStateFlow(false)
val _updateState = MutableStateFlow(UpdateState())

fun MainViewModel.openAbout() {
    _aboutVisible.value = true
}

fun MainViewModel.closeAbout() {
    _aboutVisible.value = false
    _updateState.value = UpdateState()
}

fun MainViewModel.checkForUpdates(currentVersionName: String) {
    if (_updateState.value.isChecking) return
    AppLogger.i("Update", "开始检查更新，当前版本: $currentVersionName")
    _updateState.value = UpdateState(isChecking = true)

    viewModelScope.launch {
        try {
            val result = withContext(Dispatchers.IO) {
                val apiUrl = URL("https://api.github.com/repos/y3881811-oss/ertyu/releases/latest")
                val conn = apiUrl.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                conn.setRequestProperty("User-Agent", "EYU-FileManager")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val latestTag = json.getString("tag_name")
                    val htmlUrl = json.getString("html_url")
                    
                    var apkDownloadUrl = ""
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            if (asset.optString("name").endsWith(".apk")) {
                                apkDownloadUrl = asset.optString("browser_download_url")
                                break
                            }
                        }
                    }
                    Triple(latestTag, htmlUrl, apkDownloadUrl)
                } else {
                    throw Exception("HTTP ${conn.responseCode}")
                }
            }

            val (latestTag, htmlUrl, apkDownloadUrl) = result
            val cleanLatest = latestTag.replace("v", "").replace("-", ".")
            
            if (cleanLatest != currentVersionName) {
                AppLogger.i("Update", "发现新版本: $latestTag")
                _updateState.value = UpdateState(
                    isChecking = false,
                    hasUpdate = true,
                    latestVersion = latestTag,
                    htmlUrl = htmlUrl,
                    downloadUrl = apkDownloadUrl
                )
            } else {
                AppLogger.i("Update", "当前已是最新版本")
                _updateState.value = UpdateState(isChecking = false, hasUpdate = false)
                _toast.value = "已是最新版本"
            }

        } catch (e: Exception) {
            AppLogger.e("Update", "检查更新失败", e)
            _updateState.value = UpdateState(isChecking = false, errorMsg = e.message)
            _toast.value = "检查更新失败：${e.message}"
        }
    }
}

fun MainViewModel.dismissUpdateDialog() {
    _updateState.value = _updateState.value.copy(hasUpdate = false)
}

fun MainViewModel.startDownload(url: String) {
    try {
        AppLogger.i("Update", "开始下载更新: $url")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<android.app.Application>().startActivity(intent)
    } catch (e: Exception) {
        AppLogger.e("Update", "无法打开浏览器下载", e)
        _toast.value = "无法打开浏览器"
    }
}