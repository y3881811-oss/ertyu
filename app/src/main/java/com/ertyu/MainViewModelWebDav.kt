package com.ertyu

import android.os.Environment
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.WebDavServer
import com.ertyu.model.WebDavState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.openWebDav() {
    val s = WebDavStateHolder.state.value
    if (s.sharePath.isEmpty()) {
        val default = File(
            Environment.getExternalStorageDirectory(), "Download"
        ).absolutePath
        WebDavStateHolder.state.value = s.copy(sharePath = default)
    }
    WebDavStateHolder.state.update { it.copy(error = null) }
}

fun MainViewModel.updateWebDav(
    transform: (WebDavState) -> WebDavState
) {
    WebDavStateHolder.state.update(transform)
}

fun MainViewModel.startWebDav() {
    val s = WebDavStateHolder.state.value
    if (s.isRunning) return

    val shareDir = File(s.sharePath)
    if (!shareDir.exists()) {
        WebDavStateHolder.state.update {
            it.copy(error = "共享目录不存在：${s.sharePath}")
        }
        return
    }
    if (!shareDir.isDirectory) {
        WebDavStateHolder.state.update {
            it.copy(error = "共享路径不是目录")
        }
        return
    }
    if (s.port < 1024 || s.port > 65535) {
        WebDavStateHolder.state.update {
            it.copy(error = "端口范围 1024-65535")
        }
        return
    }
    if (s.username.isEmpty() || s.password.isEmpty()) {
        WebDavStateHolder.state.update {
            it.copy(error = "用户名和密码不能为空")
        }
        return
    }

    WebDavStateHolder.state.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
        try {
            val ip = withContext(Dispatchers.IO) {
                WebDavServer.getLocalIpAddress()
            }
            val server = WebDavServer(
                port = s.port,
                rootDir = shareDir,
                username = s.username,
                password = s.password
            )
            withContext(Dispatchers.IO) {
                server.start()
            }
            WebDavStateHolder.server = server
            WebDavStateHolder.state.update {
                it.copy(
                    isRunning = true,
                    isLoading = false,
                    ipAddress = ip,
                    serverUrl = "http://$ip:${s.port}/"
                )
            }
            _toast.value = "WebDAV 已启动"
        } catch (e: Exception) {
            WebDavStateHolder.state.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "启动失败"
                )
            }
        }
    }
}

fun MainViewModel.stopWebDav() {
    viewModelScope.launch {
        try {
            withContext(Dispatchers.IO) {
                WebDavStateHolder.server?.stop()
            }
        } catch (_: Exception) {
        }
        WebDavStateHolder.server = null
        WebDavStateHolder.state.update {
            it.copy(isRunning = false, serverUrl = "")
        }
        _toast.value = "WebDAV 已停止"
    }
}

fun MainViewModel.closeWebDav() {
    WebDavStateHolder.state.value = WebDavState()
}