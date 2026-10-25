package com.ertyu.engine

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import java.io.File
import java.nio.charset.Charset

object ZipEngine {

    private const val TAG = "ZipEngine"

    suspend fun extract(
        zipFile: File,
        targetDir: File,
        password: String? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!targetDir.exists()) targetDir.mkdirs()

            val zip = ZipFile(zipFile)
            try {
                zip.setCharset(Charset.forName("UTF-8"))
            } catch (e: Exception) {
                zip.setCharset(Charset.forName("GBK"))
            }

            if (zip.isEncrypted && password.isNullOrEmpty()) {
                return@withContext Result.failure(IllegalStateException("PASSWORD_REQUIRED"))
            }

            if (!password.isNullOrEmpty()) {
                zip.setPassword(password.toCharArray())
            }

            val headers = zip.fileHeaders
            val total = headers.size
            onProgress(0, total)

            var current = 0
            for (header in headers) {
                val outFile = File(targetDir, header.fileName)
                // 防 Zip Slip 路径穿越
                if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    continue
                }
                
                try {
                    zip.extractFile(header, targetDir.absolutePath)
                } catch (e: ZipException) {
                    if (e.message?.contains("Wrong Password", ignoreCase = true) == true) {
                        return@withContext Result.failure(IllegalStateException("密码错误"))
                    }
                    // 记录错误但不中断
                    Log.e(TAG, "解压单个文件失败: ${header.fileName}", e)
                } catch (t: Throwable) {
                    Log.e(TAG, "未知错误: ${header.fileName}", t)
                }
                
                current++
                onProgress(current, total)
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "解压过程发生严重错误", t)
            Result.failure(t)
        }
    }

    suspend fun compress(
        sources: List<File>,
        targetZip: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (targetZip.exists()) targetZip.delete()
            
            val zip = ZipFile(targetZip)
            val entries = mutableListOf<Pair<File, String>>()
            sources.forEach { src -> collect(src, src.name, entries) }
            val total = entries.size
            onProgress(0, total)

            var current = 0
            entries.forEach { (file, entryName) ->
                val params = ZipParameters().apply {
                    fileNameInZip = entryName
                }
                if (file.isDirectory) {
                    zip.addFolder(file, params)
                } else {
                    zip.addFile(file, params)
                }
                current++
                onProgress(current, total)
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Log.e(TAG, "压缩过程发生严重错误", t)
            Result.failure(t)
        }
    }

    private fun collect(file: File, entryName: String, out: MutableList<Pair<File, String>>) {
        if (file.isDirectory) {
            out.add(file to entryName)
            file.listFiles()?.forEach { child ->
                collect(child, "$entryName/${child.name}", out)
            }
        } else {
            out.add(file to entryName)
        }
    }
}