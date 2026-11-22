package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.AppLogger
import com.ertyu.engine.LanScanner
import com.ertyu.model.LanScanHolder
import com.ertyu.model.LanScanState
import com.ertyu.model.SftpHolder
import com.ertyu.model.SftpState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private object LanScanJobs {
    var job: Job? = null
}

fun MainViewModel.openLanScan() {
    val self = LanScanner.localIPv4() ?: ""
    LanScanHolder.state.value = LanScanState(visible = true, selfIp = self)
    startLanScan()
}

fun MainViewModel.closeLanScan() {
    LanScanJobs.job?.cancel()
    LanScanJobs.job = null
    LanScanHolder.state.value = LanScanState()
}

fun MainViewModel.startLanScan() {
    if (LanScanHolder.state.value.scanning) return
    val self = LanScanner.localIPv4()
    if (self == null) {
        LanScanHolder.state.update { it.copy(error = "没有连上 Wi-Fi / 局域网，无法扫描") }
        return
    }
    LanScanHolder.state.update {
        it.copy(
            scanning = true, done = 0, total = 254, hosts = emptyList(),
            selfIp = self, error = null
        )
    }
    LanScanJobs.job = viewModelScope.launch {
        val result = LanScanner.scan(
            onHostFound = { host ->
                LanScanHolder.state.update { st -> st.copy(hosts = (st.hosts + host).sortedBy { it.ip }) }
            },
            onProgress = { done, total ->
                LanScanHolder.state.update { it.copy(done = done, total = total) }
            }
        )
        result.onSuccess {
            LanScanHolder.state.update { it.copy(scanning = false) }
            AppLogger.i("LanScan", "扫描完成，发现 ${LanScanHolder.state.value.hosts.size} 台设备")
        }.onFailure { e ->
            AppLogger.e("LanScan", "扫描失败", e)
            LanScanHolder.state.update { it.copy(scanning = false, error = e.message ?: "扫描失败") }
        }
    }
}

/** 用这台设备的 IP 直接进 SFTP 连接页 */
fun MainViewModel.connectSftpTo(ip: String) {
    SftpHolder.state.value = SftpState(visible = true, host = ip, port = 22)
    openSftp()
}
