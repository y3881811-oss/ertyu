package com.ertyu

import android.app.Application
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.DirectFileEngine
import com.ertyu.engine.FileEngine
import com.ertyu.model.FileItem
import com.ertyu.model.PanelState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val engine: FileEngine = DirectFileEngine()

    private val _leftPanel = MutableStateFlow(PanelState())
    val leftPanel: StateFlow<PanelState> = _leftPanel.asStateFlow()

    private val _rightPanel = MutableStateFlow(PanelState())
    val rightPanel: StateFlow<PanelState> = _rightPanel.asStateFlow()

    /** 初始化默认目录：两侧面板都打开外部存储根目录 */
    fun initDefaultDirs() {
        val root = Uri.fromFile(Environment.getExternalStorageDirectory())
        openDirectory(true, root)
        openDirectory(false, root)
    }

    fun openDirectory(isLeft: Boolean, uri: Uri) {
        val flow = if (isLeft) _leftPanel else _rightPanel
        flow.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val items = engine.list(uri)
                val parent = engine.getParent(uri)
                flow.update {
                    it.copy(
                        currentUri = uri,
                        items = items,
                        isLoading = false,
                        canGoUp = parent != null
                    )
                }
            } catch (e: Exception) {
                flow.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun goUp(isLeft: Boolean) {
        val flow = if (isLeft) _leftPanel else _rightPanel
        val current = flow.value.currentUri ?: return
        val parent = engine.getParent(current) ?: return
        openDirectory(isLeft, parent)
    }

    fun delete(isLeft: Boolean, item: FileItem) {
        val flow = if (isLeft) _leftPanel else _rightPanel
        viewModelScope.launch {
            engine.delete(item.uri)
            flow.value.currentUri?.let { openDirectory(isLeft, it) }
        }
    }

    fun copyToOtherPanel(fromLeft: Boolean, item: FileItem) {
        val targetFlow = if (fromLeft) _rightPanel else _leftPanel
        val targetUri = targetFlow.value.currentUri ?: return
        viewModelScope.launch {
            engine.copy(item.uri, targetUri)
            openDirectory(!fromLeft, targetUri)
        }
    }

    fun moveToOtherPanel(fromLeft: Boolean, item: FileItem) {
        val targetFlow = if (fromLeft) _rightPanel else _leftPanel
        val targetUri = targetFlow.value.currentUri ?: return
        viewModelScope.launch {
            engine.move(item.uri, targetUri)
            // 刷新目标面板
            openDirectory(!fromLeft, targetUri)
            // 刷新源面板
            val srcFlow = if (fromLeft) _leftPanel else _rightPanel
            srcFlow.value.currentUri?.let { openDirectory(fromLeft, it) }
        }
    }

    fun createDirectory(isLeft: Boolean, name: String) {
        val flow = if (isLeft) _leftPanel else _rightPanel
        val current = flow.value.currentUri ?: return
        viewModelScope.launch {
            engine.createDirectory(current, name)
            openDirectory(isLeft, current)
        }
    }

    fun rename(isLeft: Boolean, item: FileItem, newName: String) {
        val flow = if (isLeft) _leftPanel else _rightPanel
        viewModelScope.launch {
            engine.rename(item.uri, newName)
            flow.value.currentUri?.let { openDirectory(isLeft, it) }
        }
    }
}