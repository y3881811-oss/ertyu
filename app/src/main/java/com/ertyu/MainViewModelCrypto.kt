package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.CryptoEngine
import com.ertyu.model.CryptoMode
import com.ertyu.model.CryptoState
import com.ertyu.model.FileItem
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openCrypto(isLeft: Boolean, item: FileItem) {
    val path = item.uri.path ?: return
    val mode = if (item.name.endsWith(".enc")) CryptoMode.DECRYPT else CryptoMode.ENCRYPT
    
    CryptoStateHolder.state.value = CryptoState(
        visible = true,
        mode = mode,
        sourceIsLeft = isLeft,
        filePath = path,
        fileName = item.name
    )
}

fun MainViewModel.dismissCrypto() {
    CryptoStateHolder.state.value = CryptoState()
}

fun MainViewModel.executeCrypto(password: String) {
    val s = CryptoStateHolder.state.value
    if (s.filePath.isEmpty() || password.isEmpty()) return
    val source = File(s.filePath)
    
    CryptoStateHolder.state.update { it.copy(isProcessing = true, error = null) }
    
    viewModelScope.launch {
        val result = if (s.mode == CryptoMode.ENCRYPT) {
            CryptoEngine.encrypt(source, password)
        } else {
            CryptoEngine.decrypt(source, password)
        }
        
        result.onSuccess { outFile ->
            _toast.value = "处理完成：${outFile.name}"
            CryptoStateHolder.state.value = CryptoState() // 关闭弹窗
            refresh(s.sourceIsLeft)
        }.onFailure { e ->
            CryptoStateHolder.state.update { 
                it.copy(isProcessing = false, error = "密码错误或文件损坏: ${e.message}") 
            }
        }
    }
}