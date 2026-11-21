package com.ertyu

import com.ertyu.model.FileConflictState

/**
 * 生成不重复的文件名（用于冲突处理时的“保留两者”）
 * 例如：a.txt -> a (1).txt -> a (2).txt
 */
fun generateUniqueFileName(name: String, existingNames: Set<String>): String {
    if (!existingNames.contains(name)) return name
    
    // 简单判断是否为文件夹（无后缀）
    val isDirectory = !name.contains(".")
    val baseName = if (isDirectory) name else name.substringBeforeLast(".")
    val extension = if (isDirectory) "" else ".${name.substringAfterLast(".")}"
    
    var counter = 1
    var newName = "$baseName ($counter)$extension"
    while (existingNames.contains(newName)) {
        counter++
        newName = "$baseName ($counter)$extension"
    }
    return newName
}

/**
 * 清除文件冲突状态（重置弹窗）
 */
fun MainViewModel.clearConflictState() {
    _fileConflictState.value = FileConflictState()
}