package com.ertyu

import com.ertyu.model.ApkCloneState
import com.ertyu.model.ApkSignerState
import com.ertyu.model.DexViewerState
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 第二波（APK 深度工具）的状态。
 * 独立 object，避免修改 MainViewModel.kt。
 */
object ApkToolsState {
    val signerState = MutableStateFlow(ApkSignerState())
    val dexViewerState = MutableStateFlow(DexViewerState())
    val cloneState = MutableStateFlow(ApkCloneState())
}