package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import net.lingala.zip4j.model.ZipParameters
import java.io.BufferedInputStream
import java.io.File

object ZipEngine {

    suspend fun extract(
        zipFile: File,
        targetDir: File,
        password: String? = null,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!targetDir.exists()) targetDir.mkdirs()

            val zip = ZipFile(zipFile)
            if (zip.isEncrypted && password.isNullOrEmpty()) {
                throw IllegalStateException("PASSWORD_REQUIRED")
            }

            if (!password.isNullOrEmpty()) {
                zip.setPassword(password.toCharArray())
            }

            val total = zip.fileHeaders.size
            onProgress(0, total)

            try {
                zip.extractAll(targetDir.absolutePath)
            } catch (e: ZipException) {
                // zip4j 密码错误通常会抛出 ZipException
                if (e.message?.contains("Wrong Password", ignoreCase = true) == true) {
                    throw IllegalStateException("密码错误")
                } else {
                    throw e
                }
            }
            
            onProgress(total, total)
        }
    }

    suspend fun compress(
        sources: List<File>,
        targetZip: File,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
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