package com.ertyu

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.TaskCenter
import com.ertyu.engine.WebDavClient
import com.ertyu.model.WebDavHolder
import com.ertyu.model.WebDavState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val PREF = "eyu_webdav"

fun MainViewModel.openWebDav() {
    val app = getApplication<Application>()
    val sp = app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
    val current = WebDavHolder.state.value
    WebDavHolder.state.value = current.copy(
        visible = true,
        url = current.url.ifBlank { sp.getString("url", "") ?: "" },
        user = current.user.ifBlank { sp.getString("user", "") ?: "" }
    )
}

fun MainViewModel.closeWebDav() {
    WebDavHolder.state.value = WebDavHolder.state.value.copy(visible = false)
}

fun MainViewModel.setWebDavUrl(value: String) {
    WebDavHolder.state.update { it.copy(url = value) }
}

fun MainViewModel.setWebDavUser(value: String) {
    WebDavHolder.state.update { it.copy(user = value) }
}

fun MainViewModel.setWebDavPass(value: String) {
    WebDavHolder.state.update { it.copy(pass = value) }
}

fun MainViewModel.webDavConnect() {
    val s = WebDavHolder.state.value
    val base = WebDavClient.normalizeBase(s.url)
    if (base.isEmpty()) {
        showToast("请填写服务器地址")
        return
    }
    val app = getApplication<Application>()
    app.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        .edit().putString("url", base).putString("user", s.user).apply()

    WebDavHolder.state.update { it.copy(url = base, loading = true, error = null) }
    viewModelScope.launch {
        val r = WebDavClient.list(base, "/", s.user, s.pass)
        r.onSuccess { list ->
            AppLogger.i("WebDav", "连接成功 $base，根目录 ${list.size} 项")
            WebDavHolder.state.update {
                it.copy(connected = true, path = "/", entries = list, loading = false, error = null)
            }
        }.onFailure { e ->
            AppLogger.e("WebDav", "连接失败 $base", e)
            WebDavHolder.state.update { it.copy(loading = false, connected = false, error = e.message) }
        }
    }
}

fun MainViewModel.webDavRefresh() {
    loadWebDavDir(WebDavHolder.state.value.path)
}

fun MainViewModel.webDavOpenEntry(entry: WebDavClient.Entry) {
    if (entry.isDir) loadWebDavDir(entry.path) else webDavDownload(entry)
}

fun MainViewModel.webDavUp() {
    val path = WebDavHolder.state.value.path.trimEnd('/')
    if (path.isEmpty()) return
    val parent = path.substringBeforeLast('/', "").ifEmpty { "/" }
    loadWebDavDir(if (parent.endsWith("/")) parent else "$parent/")
}

private fun MainViewModel.loadWebDavDir(path: String) {
    val s = WebDavHolder.state.value
    val base = WebDavClient.normalizeBase(s.url)
    WebDavHolder.state.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
        val r = WebDavClient.list(base, path, s.user, s.pass)
        r.onSuccess { list ->
            WebDavHolder.state.update {
                it.copy(path = path, entries = list, loading = false, error = null)
            }
        }.onFailure { e ->
            AppLogger.e("WebDav", "列出 $path 失败", e)
            WebDavHolder.state.update { it.copy(loading = false, error = e.message) }
        }
    }
}

fun MainViewModel.webDavDownload(entry: WebDavClient.Entry) {
    val s = WebDavHolder.state.value
    if (s.busyText.isNotBlank()) {
        showToast("已有传输在进行")
        return
    }
    val base = WebDavClient.normalizeBase(s.url)
    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "EYU-WebDAV")
    val target = File(dir, entry.path.trimEnd('/').substringAfterLast('/').ifBlank { "download" })
    if (target.exists()) {
        showToast("已存在同名文件：${target.name}")
        return
    }
    val taskId = TaskCenter.start("下载 ${entry.name}", "WebDAV", null)
    WebDavHolder.state.update { it.copy(busyText = "下载 ${entry.name}…") }
    viewModelScope.launch {
        val r = WebDavClient.download(base, entry, target, s.user, s.pass) { p ->
            WebDavHolder.state.update { it.copy(busyText = "下载 ${entry.name} ${(p * 100).toInt()}%") }
        }
        r.onSuccess {
            TaskCenter.finish(taskId, true, "已下载")
            WebDavHolder.state.update { it.copy(busyText = "") }
            showToast("已保存到 Download/EYU-WebDAV/${target.name}")
        }.onFailure { e ->
            TaskCenter.finish(taskId, false, e.message ?: "下载失败")
            WebDavHolder.state.update { it.copy(busyText = "", error = e.message) }
            showToast("下载失败：${e.message}")
        }
    }
}

fun MainViewModel.webDavUpload(uris: List<Uri>) {
    val s = WebDavHolder.state.value
    if (!s.connected) {
        showToast("请先连接服务器")
        return
    }
    val base = WebDavClient.normalizeBase(s.url)
    val app = getApplication<Application>()
    val cacheDir = File(app.cacheDir, "webdav_upload").apply { mkdirs() }
    val taskId = TaskCenter.start("上传 ${uris.size} 个文件", "WebDAV", null)

    viewModelScope.launch {
        var ok = 0
        var fail = 0
        for ((index, uri) in uris.withIndex()) {
            val name = queryDisplayName(uri) ?: "upload_${System.currentTimeMillis()}"
            WebDavHolder.state.update { it.copy(busyText = "上传 ${index + 1}/${uris.size}：$name") }
            val local = File(cacheDir, name)
            val copied = withContext(Dispatchers.IO) {
                try {
                    app.contentResolver.openInputStream(uri)?.use { input ->
                        local.outputStream().use { out -> input.copyTo(out) }
                    }
                    local.isFile && local.length() > 0
                } catch (e: Exception) {
                    AppLogger.e("WebDav", "读取待上传文件失败 $uri", e)
                    false
                }
            }
            if (!copied) {
                fail++
                continue
            }
            val remotePath = s.path.trimEnd('/') + "/" + name
            val r = WebDavClient.upload(base, remotePath, local, s.user, s.pass)
            if (r.isSuccess) ok++ else {
                fail++
                AppLogger.e("WebDav", "上传失败 $remotePath", r.exceptionOrNull())
            }
            local.delete()
        }
        TaskCenter.finish(taskId, ok > 0, "成功 $ok 个${if (fail > 0) "，失败 $fail 个" else ""}")
        WebDavHolder.state.update { it.copy(busyText = "") }
        showToast(if (fail == 0) "上传完成 $ok 个文件" else "上传完成：成功 $ok，失败 $fail")
        loadWebDavDir(WebDavHolder.state.value.path)
    }
}

private fun MainViewModel.queryDisplayName(uri: Uri): String? = try {
    getApplication<Application>().contentResolver.query(uri, null, null, null, null)?.use { c ->
        if (c.moveToFirst()) {
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) c.getString(idx) else null
        } else null
    }
} catch (_: Exception) {
    null
}
