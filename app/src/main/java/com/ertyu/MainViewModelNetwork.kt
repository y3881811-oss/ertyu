package com.ertyu

import android.os.Environment
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.FtpServerManager
import com.ertyu.engine.WebShareServer
import com.ertyu.model.NetworkShareState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

fun MainViewModel.openNetworkShare() {
    val s = NetworkShareStateHolder.state.value
    if (s.sharePath.isEmpty()) {
        val default = File(Environment.getExternalStorageDirectory(), "Download").absolutePath
        NetworkShareStateHolder.state.value = s.copy(sharePath = default)
    }
    NetworkShareStateHolder.state.update { it.copy(visible = true, error = null) }
}

fun MainViewModel.closeNetworkShare() {
    if (NetworkShareStateHolder.state.value.isRunning) {
        stopNetworkShare()
    }
    NetworkShareStateHolder.state.value = NetworkShareState()
}

fun MainViewModel.updateNetworkShare(transform: (NetworkShareState) -> NetworkShareState) {
    NetworkShareStateHolder.state.update(transform)
}

fun MainViewModel.startNetworkShare() {
    val s = NetworkShareStateHolder.state.value
    if (s.isRunning) return

    val shareDir = File(s.sharePath)
    if (!shareDir.exists() || !shareDir.isDirectory) {
        NetworkShareStateHolder.state.update { it.copy(error = "共享目录无效") }
        return
    }

    NetworkShareStateHolder.state.update { it.copy(isLoading = true, error = null) }

    viewModelScope.launch {
        try {
            val ip = withContext(Dispatchers.IO) { WebShareServer.getLocalIpAddress() }
            
            // 启动 FTP
            withContext(Dispatchers.IO) {
                FtpServerManager.start(s.ftpPort, s.ftpUsername, s.ftpPassword, shareDir)
                    .getOrThrow()
            }

            // 启动 Web HTTP
            val web = WebShareServer(s.httpPort, shareDir)
            withContext(Dispatchers.IO) { web.start() }
            NetworkShareStateHolder.webServer = web

            NetworkShareStateHolder.state.update {
                it.copy(isRunning = true, isLoading = false, ipAddress = ip)
            }
            _toast.value = "局域网共享已启动"
        } catch (e: Exception) {
            // 如果 FTP 启动失败，尝试停止可能已启动的
            FtpServerManager.stop()
            NetworkShareStateHolder.state.update {
                it.copy(isLoading = false, error = "启动失败: ${e.message}")
            }
        }
    }
}

fun MainViewModel.stopNetworkShare() {
    viewModelScope.launch {
        withContext(Dispatchers.IO) {
            FtpServerManager.stop()
            NetworkShareStateHolder.webServer?.stop()
        }
        NetworkShareStateHolder.webServer = null
        NetworkShareStateHolder.state.update { it.copy(isRunning = false) }
        _toast.value = "局域网共享已停止"
    }
}