package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.ItemSizeEngine
import com.ertyu.engine.RemoteEntry
import com.ertyu.engine.SftpEngine
import com.ertyu.engine.SftpSession
import com.ertyu.model.SftpHolder
import com.ertyu.model.SftpState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

private object SftpRuntime {
    var session: SftpSession? = null
}

fun MainViewModel.openSftp() {
    SftpHolder.state.value = SftpState(
        visible = true,
        host = SftpHolder.state.value.host,
        port = SftpHolder.state.value.port,
        user = SftpHolder.state.value.user
    )
}

fun MainViewModel.closeSftp() {
    SftpRuntime.session?.close()
    SftpRuntime.session = null
    SftpHolder.state.value = SftpState()
}

fun MainViewModel.sftpConnect(host: String, port: Int, user: String, password: String) {
    val snap = SftpHolder.state.value
    if (snap.connecting) return
    if (host.isBlank() || user.isBlank()) {
        showToast("请填写主机和用户名")
        return
    }
    SftpRuntime.session?.close()
    SftpRuntime.session = null
    SftpHolder.state.update {
        it.copy(
            connecting = true, connected = false, error = null,
            host = host, port = port, user = user
        )
    }
    viewModelScope.launch {
        val result = SftpEngine.connect(host, port, user, password)
        result.onSuccess { session ->
            SftpRuntime.session = session
            SftpHolder.state.update { it.copy(connecting = false, connected = true) }
            AppLogger.i("Sftp", "已连接 $user@$host:$port")
            sftpOpen("/")
        }.onFailure { e ->
            AppLogger.e("Sftp", "连接失败 $user@$host:$port", e)
            SftpHolder.state.update {
                it.copy(connecting = false, error = e.message ?: "连接失败，请检查地址/账号/密码")
            }
        }
    }
}

fun MainViewModel.sftpOpen(path: String) {
    val session = SftpRuntime.session ?: return
    SftpHolder.state.update { it.copy(loading = true, error = null) }
    viewModelScope.launch {
        SftpEngine.list(session, path).onSuccess { list ->
            SftpHolder.state.update { it.copy(loading = false, path = path, entries = list) }
        }.onFailure { e ->
            SftpHolder.state.update { it.copy(loading = false, error = e.message ?: "读取目录失败") }
        }
    }
}

fun MainViewModel.sftpGoUp() {
    val path = SftpHolder.state.value.path
    if (path == "/" || path.isBlank()) return
    val parent = path.trimEnd('/').substringBeforeLast('/', "")
    sftpOpen(if (parent.isEmpty()) "/" else parent)
}

fun MainViewModel.sftpRefresh() {
    sftpOpen(SftpHolder.state.value.path)
}

/** 把远端文件下载到左侧当前目录（重名自动改名，绝不覆盖） */
fun MainViewModel.sftpDownload(entry: RemoteEntry) {
    val session = SftpRuntime.session ?: return
    val localDir = leftPanel.value.currentUri?.path?.let(::File)
    if (localDir == null) {
        showToast("左面板没有打开目录")
        return
    }
    val existing = localDir.list()?.toSet() ?: emptySet()
    val finalName = generateUniqueFileName(entry.name, existing)
    val target = File(localDir, finalName)
    SftpHolder.state.update { it.copy(busyText = "正在下载 ${entry.name}") }
    viewModelScope.launch {
        SftpEngine.download(session, entry.path, target) { done ->
            val total = entry.size.coerceAtLeast(1)
            SftpHolder.state.update {
                it.copy(busyText = "正在下载 ${entry.name}（${ItemSizeEngine.format(done)}/${ItemSizeEngine.format(total)}）")
            }
        }.onSuccess {
            SftpHolder.state.update { it.copy(busyText = "") }
            showToast(if (finalName == entry.name) "已下载 ${entry.name}" else "已下载为 $finalName（同名已存在）")
            invalidateSizeFor(localDir.absolutePath)
            refresh(true)
        }.onFailure { e ->
            AppLogger.e("Sftp", "下载失败 ${entry.path}", e)
            SftpHolder.state.update { it.copy(busyText = "", error = e.message ?: "下载失败") }
        }
    }
}

/** 把左面板选中的文件上传到当前远端目录（重名自动改名） */
fun MainViewModel.sftpUploadSelected() {
    val session = SftpRuntime.session ?: return
    val snap = SftpHolder.state.value
    val panel = leftPanel.value
    val items = panel.items.filter { panel.selectedUris.contains(it.uriString) && !it.isDirectory }
    if (items.isEmpty()) {
        showToast("请先在左面板选中要上传的文件（文件夹暂不支持）")
        return
    }
    val remoteDir = snap.path
    viewModelScope.launch {
        var ok = 0
        var fail = 0
        var renamed = 0
        val used = snap.entries.map { it.name }.toMutableSet()
        for (item in items) {
            val local = File(item.path)
            if (!local.isFile) {
                fail++
                continue
            }
            val name = generateUniqueFileName(item.name, used)
            if (name != item.name) renamed++
            used.add(name)
            val remotePath = if (remoteDir.endsWith("/")) "$remoteDir$name" else "$remoteDir/$name"
            SftpHolder.state.update { it.copy(busyText = "正在上传 ${item.name}") }
            val r = SftpEngine.upload(session, local, remotePath) { done ->
                SftpHolder.state.update {
                    it.copy(busyText = "正在上传 ${item.name}（${ItemSizeEngine.format(done)}）")
                }
            }
            if (r.isSuccess) ok++ else {
                fail++
                AppLogger.e("Sftp", "上传失败 ${item.path} -> $remotePath", r.exceptionOrNull())
            }
        }
        SftpHolder.state.update { it.copy(busyText = "") }
        showToast(
            buildString {
                append("上传完成：成功 $ok 项")
                if (fail > 0) append("，失败 $fail 项")
                if (renamed > 0) append("，$renamed 项因重名自动改名")
            }
        )
        sftpRefresh()
    }
}
