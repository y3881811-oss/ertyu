package com.ertyu

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ApkCloner
import com.ertyu.engine.ApkParserEngine
import com.ertyu.engine.ApkSignerEngine
import com.ertyu.engine.DexDecompiler
import com.ertyu.model.ApkCloneState
import com.ertyu.model.ApkSignerState
import com.ertyu.model.DexViewerState
import com.ertyu.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// ==================== APK 签名 ====================

fun MainViewModel.openApkSigner(item: FileItem) {
    val path = item.uri.path ?: return
    ApkToolsState.signerState.value = ApkSignerState(
        visible = true, apkPath = path, apkName = item.name
    )
}

fun MainViewModel.closeApkSigner() {
    ApkToolsState.signerState.value = ApkSignerState()
}

fun MainViewModel.updateApkSigner(transform: (ApkSignerState) -> ApkSignerState) {
    ApkToolsState.signerState.update(transform)
}

fun MainViewModel.executeApkSign() {
    val state = ApkToolsState.signerState.value
    if (state.isSigning) return
    if (state.keystorePath.isEmpty()) { _toast.value = "请先选择 keystore 文件"; return }
    if (state.storePass.isEmpty()) { _toast.value = "请输入 store 密码"; return }

    val apkFile = File(state.apkPath)
    val keystoreFile = File(state.keystorePath)
    val outputApk = File(apkFile.parentFile, apkFile.nameWithoutExtension + "_signed.apk")

    viewModelScope.launch {
        ApkToolsState.signerState.update { it.copy(isSigning = true, error = null) }
        val result = ApkSignerEngine.sign(
            inputApk = apkFile, outputApk = outputApk, keystoreFile = keystoreFile,
            storePass = state.storePass, keyAlias = state.keyAlias, keyPass = state.keyPass.ifEmpty { state.storePass }
        ) { msg ->
            ApkToolsState.signerState.update { it.copy(progressMessage = msg) }
        }
        result.onSuccess {
            ApkToolsState.signerState.update { it.copy(isSigning = false, resultPath = outputApk.absolutePath, progressMessage = "完成：${outputApk.name}") }
            _toast.value = "签名完成：${outputApk.name}"
        }.onFailure { e ->
            ApkToolsState.signerState.update { it.copy(isSigning = false, error = e.message ?: "签名失败") }
        }
    }
}

// ==================== DEX 反编译 ====================

fun MainViewModel.openDexViewerFromApk(apkPath: String, dexPath: String) {
    val apkFile = File(apkPath)
    val dexName = dexPath.substringAfterLast('/')
    ApkToolsState.dexViewerState.value = DexViewerState(
        visible = true, apkPath = apkPath, apkName = apkFile.name, dexName = dexName, isLoading = true
    )
    viewModelScope.launch {
        val result = DexDecompiler.listClasses(apkFile, dexName)
        result.onSuccess { classes ->
            ApkToolsState.dexViewerState.update { it.copy(isLoading = false, classes = classes) }
        }.onFailure { e ->
            ApkToolsState.dexViewerState.update { it.copy(isLoading = false, error = e.message ?: "读取失败") }
        }
    }
}

fun MainViewModel.openDexViewer(apkItem: FileItem, dexName: String) {
    val path = apkItem.uri.path ?: return
    ApkToolsState.dexViewerState.value = DexViewerState(
        visible = true, apkPath = path, apkName = apkItem.name, dexName = dexName, isLoading = true
    )
    viewModelScope.launch {
        val result = DexDecompiler.listClasses(File(path), dexName)
        result.onSuccess { classes ->
            ApkToolsState.dexViewerState.update { it.copy(isLoading = false, classes = classes) }
        }.onFailure { e ->
            ApkToolsState.dexViewerState.update { it.copy(isLoading = false, error = e.message ?: "读取失败") }
        }
    }
}

fun MainViewModel.closeDexViewer() { ApkToolsState.dexViewerState.value = DexViewerState() }
fun MainViewModel.updateDexFilter(q: String) { ApkToolsState.dexViewerState.update { it.copy(filter = q) } }

fun MainViewModel.openSmaliClass(classType: String) {
    val state = ApkToolsState.dexViewerState.value
    ApkToolsState.dexViewerState.update { it.copy(selectedClass = classType, isLoadingSmali = true, smaliContent = "", error = null) }
    viewModelScope.launch {
        val app = getApplication<Application>()
        val result = DexDecompiler.decompileClass(
            apkFile = File(state.apkPath), dexName = state.dexName, classType = classType, cacheDir = app.cacheDir
        )
        result.onSuccess { content ->
            ApkToolsState.dexViewerState.update { it.copy(isLoadingSmali = false, smaliContent = content) }
        }.onFailure { e ->
            ApkToolsState.dexViewerState.update { it.copy(isLoadingSmali = false, error = e.message ?: "反编译失败") }
        }
    }
}

fun MainViewModel.closeSmaliClass() { ApkToolsState.dexViewerState.update { it.copy(selectedClass = null, smaliContent = "") } }

// ==================== APK 共存/克隆 ====================

fun MainViewModel.openApkClone(item: FileItem) {
    val path = item.uri.path ?: return
    viewModelScope.launch {
        val pkgName = withContext(Dispatchers.IO) {
            runCatching {
                val app = getApplication<Application>()
                app.packageManager.getPackageArchiveInfo(path, 0)?.packageName
            }.getOrNull()
        }
        if (pkgName.isNullOrBlank()) { _toast.value = "无法读取包名"; return@launch }
        ApkToolsState.cloneState.value = ApkCloneState(
            visible = true, apkPath = path, apkName = item.name, originalPackage = pkgName
        )
    }
}

fun MainViewModel.closeApkClone() { ApkToolsState.cloneState.value = ApkCloneState() }
fun MainViewModel.updateApkClone(transform: (ApkCloneState) -> ApkCloneState) { ApkToolsState.cloneState.update(transform) }

fun MainViewModel.executeApkClone(keystoreFile: File, storePass: String, keyAlias: String, keyPass: String) {
    val state = ApkToolsState.cloneState.value
    if (state.isProcessing) return
    if (!state.lengthMatches) { ApkToolsState.cloneState.update { it.copy(error = "新包名长度必须与原包名相同") }; return }
    if (state.newPackage == state.originalPackage) { ApkToolsState.cloneState.update { it.copy(error = "新包名不能与原包名相同") }; return }

    val apkFile = File(state.apkPath)
    val outputApk = File(apkFile.parentFile, apkFile.nameWithoutExtension + "_cloned.apk")

    viewModelScope.launch {
        ApkToolsState.cloneState.update { it.copy(isProcessing = true, error = null) }
        val result = ApkCloner.clone(
            apkFile = apkFile, outputApk = outputApk, oldPackage = state.originalPackage,
            newPackage = state.newPackage, keystoreFile = keystoreFile, storePass = storePass,
            keyAlias = keyAlias, keyPass = keyPass
        ) { msg ->
            ApkToolsState.cloneState.update { it.copy(progressMessage = msg) }
        }
        result.onSuccess {
            ApkToolsState.cloneState.update { it.copy(isProcessing = false, resultPath = outputApk.absolutePath, progressMessage = "完成：${outputApk.name}") }
            _toast.value = "克隆完成：${outputApk.name}"
        }.onFailure { e ->
            ApkToolsState.cloneState.update { it.copy(isProcessing = false, error = e.message ?: "克隆失败") }
        }
    }
}

