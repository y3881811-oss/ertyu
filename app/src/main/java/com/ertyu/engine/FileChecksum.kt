package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object FileChecksum {

    suspend fun calculate(
        file: File,
        algorithm: String,
        onProgress: (Float) -> Unit = {}
    ): String? = withContext(Dispatchers.IO) {
        try {
            val md = MessageDigest.getInstance(algorithm)
            FileInputStream(file).use { input ->
                val total = file.length()
                val buf = ByteArray(64 * 1024)
                var read = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    md.update(buf, 0, n)
                    read += n
                    if (total > 0) onProgress(read.toFloat() / total)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}