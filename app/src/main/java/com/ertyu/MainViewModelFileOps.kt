package com.ertyu

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ArchiveEngine
import com.ertyu.model.ArchiveFormat
import com.ertyu.model.FileItem
import com.ertyu.model.ZipProgress
import com.ertyu.model.ZipPrompt
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.dismissZipPrompt() {
    _zipPrompt.value = ZipPrompt()
}

// ================== 新增：用于保存需要重试密码的解压参数 ==================
data class PendingExtractReq(
    val isLeft: Boolean,
    val item: FileItem,
    val targetDirName: String,
    val toOtherPanel: Boolean,
    val deleteSource: Boolean
)
private var pendingExtractReq: PendingExtractReq? = null

/**
 * 统一的解压入口
 * @param targetDirName 目标文件夹名（如果为空，则解压到当前目录，不建新文件夹）
 * @param toOtherPanel 是否解压到另一标签页（对侧面板）
 * @param deleteSource 是否解压后删除源文件
 */
fun MainViewModel.extractArchive(
    isLeft: Boolean,
    item: FileItem,
    targetDirName: String,
    toOtherPanel: Boolean,
    deleteSource: Boolean,
    password: String? = null
) {
    _zipPrompt.value = ZipPrompt()
    
    val targetIsLeft = if (toOtherPanel) !isLeft else isLeft
    val targetParentDir = flowOf(targetIsLeft).value.currentUri?.path?.let(::File) ?: run {
        _toast.value = "目标面板无有效路径"
        return
    }
    
    // 确定最终的解压目标目录
    val finalTargetDir = if (targetDirName.isNotBlank()) {
        File(targetParentDir, targetDirName)
    } else {
        targetParentDir
    }

    val archiveFile = item.uri.path?.let(::File) ?: return

    // 保存请求，以便密码错误时可以重试
    pendingExtractReq = PendingExtractReq(isLeft, item, targetDirName, toOtherPanel, deleteSource)

    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "解压中", message = archiveFile.name)
        val result = ArchiveEngine.extract(archiveFile, finalTargetDir, password) { cur, total ->
            _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
        }
        
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "解压完成")
            
            // 👇 1. 提示解压后的路径
            _toast.value = "已解压至: ${finalTargetDir.absolutePath}"
            
            // 👇 2. 处理源文件删除
            if (deleteSource) {
                if (engine.delete(item.uri)) {
                    _toast.value = "已解压至: ${finalTargetDir.absolutePath}，且删除了源文件"
                }
            }
            
            // 👇 3. 刷新并跳转到解压目录
            refresh(targetIsLeft)
            openDirectory(targetIsLeft, Uri.fromFile(finalTargetDir))
            pendingExtractReq = null
            
        }.onFailure { e ->
            if (e.message == "PASSWORD_REQUIRED") {
                _zipProgress.value = _zipProgress.value.copy(
                    needsPassword = true, 
                    error = "文件已加密",
                    pendingFileItem = item,
                    pendingIsLeft = isLeft,
                    pendingTargetIsLeft = targetIsLeft
                )
            } else {
                _zipProgress.value = _zipProgress.value.copy(error = "解压失败: ${e.message ?: "未知错误"}")
            }
        }
    }
}

// 原有的快捷解压（解压到当前目录的以包名命名的文件夹）
fun MainViewModel.extractZipHere(isLeft: Boolean, item: FileItem, password: String? = null) {
    val archiveFile = item.uri.path?.let(::File) ?: return
    extractArchive(isLeft, item, archiveBaseName(archiveFile.name), toOtherPanel = false, deleteSource = false, password = password)
}

// 原有的快捷解压（解压到另一侧）
fun MainViewModel.extractZipToOtherPanel(isLeft: Boolean, item: FileItem, password: String? = null) {
    val archiveFile = item.uri.path?.let(::File) ?: return
    extractArchive(isLeft, item, archiveBaseName(archiveFile.name), toOtherPanel = true, deleteSource = false, password = password)
}

fun MainViewModel.submitZipPassword(password: String) {
    val state = _zipProgress.value
    val req = pendingExtractReq ?: return // 从暂存的请求中取数据

    _zipProgress.update { it.copy(needsPassword = false, error = null) }

    extractArchive(
        isLeft = req.isLeft,
        item = req.item,
        targetDirName = req.targetDirName,
        toOtherPanel = req.toOtherPanel,
        deleteSource = req.deleteSource,
        password = password
    )
}

private fun archiveBaseName(fileName: String): String {
    val lower = fileName.lowercase()
    return when {
        lower.endsWith(".tar.gz") -> fileName.dropLast(7)
        lower.endsWith(".tar.bz2") -> fileName.dropLast(8)
        lower.endsWith(".tar.xz") -> fileName.dropLast(7)
        lower.endsWith(".tar.zst") -> fileName.dropLast(8)
        lower.endsWith(".tar") -> fileName.dropLast(4)
        lower.endsWith(".tgz") -> fileName.dropLast(4)
        lower.endsWith(".tbz2") -> fileName.dropLast(5)
        lower.endsWith(".tbz") -> fileName.dropLast(4)
        lower.endsWith(".txz") -> fileName.dropLast(4)
        lower.endsWith(".tzst") -> fileName.dropLast(5)
        else -> fileName.substringBeforeLast('.', fileName)
    }
}

// ==================== 压缩逻辑保持不变 ====================
fun MainViewModel.requestCompress(isLeft: Boolean) {
    val state = flowOf(isLeft).value
    if (state.selectedUris.isEmpty()) return
    _compressPanelIsLeft.value = isLeft
    _compressPromptVisible.value = true
}

fun MainViewModel.dismissCompressPrompt() {
    _compressPromptVisible.value = false
}

fun MainViewModel.compressWithFormat(format: ArchiveFormat) {
    _compressPromptVisible.value = false
    if (!format.canCompress) return
    val isLeft = _compressPanelIsLeft.value
    val state = flowOf(isLeft).value
    val currentDir = state.currentUri?.path?.let(::File) ?: return
    val targets = state.items.filter { state.selectedUris.contains(it.uriString) }
    if (targets.isEmpty()) return
    val sources = targets.mapNotNull { it.uri.path?.let(::File) }
    if (sources.isEmpty()) return

    val ext = when (format) {
        ArchiveFormat.ZIP -> ".zip"
        ArchiveFormat.TAR -> ".tar"
        ArchiveFormat.TAR_GZ -> ".tar.gz"
        ArchiveFormat.SEVEN_Z -> ".7z"
        else -> ".zip"
    }

    val fileName = if (sources.size == 1) {
        val n = sources[0].name
        val base = if (n.contains('.')) n.substringBeforeLast('.') else n
        "$base$ext"
    } else {
        "archive_${System.currentTimeMillis()}$ext"
    }

    val targetFile = File(currentDir, fileName)

    viewModelScope.launch {
        _zipProgress.value = ZipProgress(visible = true, title = "压缩中", message = fileName)
        val result = ArchiveEngine.compress(sources, targetFile, format) { cur, total ->
            _zipProgress.value = _zipProgress.value.copy(current = cur, total = total)
        }
        result.onSuccess {
            _zipProgress.value = _zipProgress.value.copy(done = true, message = "压缩完成：$fileName")
        }.onFailure { e ->
            _zipProgress.value = _zipProgress.value.copy(error = "压缩失败: ${e.message ?: "未知错误"}")
        }
        clearSelection(isLeft)
        refresh(isLeft)
    }
}

fun MainViewModel.dismissZipProgress() {
    _zipProgress.value = ZipProgress()
}