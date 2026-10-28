package com.ertyu

import androidx.lifecycle.viewModelScope
import com.ertyu.engine.ImageUtils
import com.ertyu.model.FileItem
import kotlinx.coroutines.launch
import java.io.File

fun MainViewModel.openImageTools(item: FileItem) {
    _imageToolTarget.value = item
}

fun MainViewModel.dismissImageTools() {
    _imageToolTarget.value = null
}

fun MainViewModel.compressImage(item: FileItem, quality: Int) {
    val path = item.uri.path ?: return
    _imageToolProcessing.value = true
    
    viewModelScope.launch {
        val outFile = ImageUtils.compressImage(File(path), quality)
        _imageToolProcessing.value = false
        _imageToolTarget.value = null
        
        if (outFile != null) {
            _toast.value = "已生成：${outFile.name}"
            refresh(true)
            refresh(false)
        } else {
            _toast.value = "压缩失败"
        }
    }
}

fun MainViewModel.renameToMd5(item: FileItem, isLeft: Boolean) {
    val path = item.uri.path ?: return
    viewModelScope.launch {
        val md5 = ImageUtils.getMd5(File(path))
        if (md5 != null) {
            val newName = "$md5.${item.name.substringAfterLast('.', "bin")}"
            rename(isLeft, item, newName)
            _toast.value = "已更名为：$newName"
        } else {
            _toast.value = "计算 MD5 失败"
        }
    }
}