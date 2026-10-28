package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.RecycleBinManager
import com.ertyu.model.RecycleBinItem
import com.ertyu.model.RecycleBinState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openRecycleBin() {
    RecycleBinStateHolder.state.value = RecycleBinState(visible = true, isLoading = true)
    viewModelScope.launch {
        val items = RecycleBinManager.getItems(getApplication())
        RecycleBinStateHolder.state.update { it.copy(items = items, isLoading = false) }
    }
}

fun MainViewModel.closeRecycleBin() {
    RecycleBinStateHolder.state.value = RecycleBinState()
}

fun MainViewModel.restoreFromTrash(item: RecycleBinItem) {
    viewModelScope.launch {
        val ok = RecycleBinManager.restore(getApplication(), item)
        if (ok) {
            _toast.value = "已还原：${item.name}"
            openRecycleBin() // 刷新列表
            refresh(true)
            refresh(false)
        } else {
            _toast.value = "还原失败，原路径可能被占用"
        }
    }
}

fun MainViewModel.deleteFromTrash(item: RecycleBinItem) {
    viewModelScope.launch {
        RecycleBinManager.deletePermanently(getApplication(), item)
        openRecycleBin()
    }
}

fun MainViewModel.clearRecycleBin() {
    viewModelScope.launch {
        RecycleBinManager.clearAll(getApplication())
        RecycleBinStateHolder.state.update { it.copy(items = emptyList()) }
    }
}