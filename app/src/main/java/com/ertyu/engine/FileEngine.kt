package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem

interface FileEngine {
    suspend fun list(uri: Uri): List<FileItem>
    suspend fun delete(uri: Uri): Boolean
    suspend fun createDirectory(parent: Uri, name: String): Uri?
    suspend fun rename(uri: Uri, newName: String): Uri?
    suspend fun copy(source: Uri, targetDir: Uri): Boolean
    suspend fun move(source: Uri, targetDir: Uri): Boolean
    fun getParent(uri: Uri): Uri?
    fun getDisplayName(uri: Uri): String

    // 新增：文本读写
    suspend fun readText(uri: Uri): String?
    suspend fun writeText(uri: Uri, content: String): Boolean
}