package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.SmbFileEngine
import com.ertyu.model.SmbConfig
import com.ertyu.model.SmbConnectState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object SmbStateHolder {
    val state = MutableStateFlow(SmbConnectState())
}

fun MainViewModel.openSmbDialog() {
    SmbStateHolder.state.value = SmbConnectState(visible = true)
}

fun MainViewModel.closeSmbDialog() {
    SmbStateHolder.state.value = SmbConnectState()
}

fun MainViewModel.connectSmb(config: SmbConfig) {
    SmbStateHolder.state.update { it.copy(isLoading = true, error = null) }
    
    viewModelScope.launch {
        try {
            val testEngine = SmbFileEngine(config)
            val rootUri = Uri.parse("smb://${config.host}/${config.shareName}/")
            
            // 尝试列一下根目录来测试连接
            val testList = testEngine.list(rootUri)
            
            // 切换引擎
            engine = testEngine
            SmbStateHolder.state.value = SmbConnectState()
            
            // 刷新面板到 SMB 根目录
            openDirectory(true, rootUri)
            openDirectory(false, rootUri)
            _toast.value = "SMB 连接成功"
        } catch (e: Exception) {
            SmbStateHolder.state.update {
                it.copy(isLoading = false, error = e.message ?: "连接失败")
            }
        }
    }
}