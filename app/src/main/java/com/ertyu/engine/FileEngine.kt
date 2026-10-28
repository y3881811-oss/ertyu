package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import com.ertyu.model.TransferProgress

interface FileEngine {
    suspend fun list(uri: Uri): List<FileItem>
    suspend fun delete(uri: Uri): Boolean
    suspend fun createDirectory(parent: Uri, name: String): Uri?
    suspend fun rename(uri: Uri, newName: String): Uri?
    
    // 加入了回调
    suspend fun copy(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit = {}): Boolean
    suspend fun move(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit = {}): Boolean
    
    fun getParent(uri: Uri): Uri?
    fun getDisplayName(uri: Uri): String
    suspend fun readText(uri: Uri): String?
    suspend fun writeText(uri: Uri, content: String): Boolean
}