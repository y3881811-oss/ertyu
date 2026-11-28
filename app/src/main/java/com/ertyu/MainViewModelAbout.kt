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

            if (com.ertyu.engine.UpdatePrefs.shouldSkip(getApplication(), latestTag)) {
                AppLogger.i("Update", "已跳过版本 $latestTag（7 天内不再提示）")
                _updateState.value = UpdateState(isChecking = false, hasUpdate = false)
                return@launch
            }
            val newer = compareVersions(latestTag, currentVersionName) > 0

            if (newer) {
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
    // 改为 App 内置下载：SHA256 + 签名校验都通过才触发安装，不再交给浏览器（避免代理替换 APK）
    startUpdateDownload(url)
}

/**
 * 把 "v2026.11.22-1257" / "2026.11.21.1302" 拆成数字段逐段比较。
 * 返回 1 表示 latest 更新，0 相同，-1 latest 更旧（避免字符串比较把回滚也当更新）。
 */
private fun versionSegments(tag: String): List<Long> =
    tag.lowercase().removePrefix("v")
        .split('.', '-', '_', '+')
        .mapNotNull { it.trim().toLongOrNull() }

private fun compareVersions(latestTag: String, current: String): Int {
    val a = versionSegments(latestTag)
    val b = versionSegments(current)
    if (a.isEmpty() || b.isEmpty()) return if (latestTag == current) 0 else 1
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0L }
        val y = b.getOrElse(i) { 0L }
        if (x != y) return if (x > y) 1 else -1
    }
    return 0
}

/** 用户选择「跳过此版本」：同一 tag 7 天内不再提示 */
fun MainViewModel.skipThisUpdate() {
    val tag = _updateState.value.latestVersion
    if (tag.isBlank()) return
    com.ertyu.engine.UpdatePrefs.skip(getApplication(), tag)
    _updateState.value = _updateState.value.copy(hasUpdate = false)
    _toast.value = "已跳过 $tag，7 天内不再提示"
    AppLogger.i("Update", "用户跳过版本 $tag")
}
