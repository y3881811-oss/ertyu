package com.ertyu.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoEngine {

    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val IV = "MTFM_DEFAULT_IV_16" // 16 字节

    private fun getKey(password: String): SecretKeySpec {
        // 使用 SHA-256 生成 32 字节的 AES-256 密钥
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = password.toByteArray(Charsets.UTF_8)
        digest.update(bytes)
        return SecretKeySpec(digest.digest(), "AES")
    }

    suspend fun encrypt(
        source: File,
        password: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(source.parentFile, source.name + ".enc")
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getKey(password), IvParameterSpec(IV.toByteArray()))

            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    val total = source.length()
                    var readTotal = 0L
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        val outBytes = cipher.update(buf, 0, read)
                        if (outBytes != null) output.write(outBytes)
                        readTotal += read
                        onProgress(readTotal.toFloat() / total)
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) output.write(finalBytes)
                }
            }
            target
        }
    }

    suspend fun decrypt(
        source: File,
        password: String,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val target = File(source.parentFile, source.name.removeSuffix(".enc"))
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getKey(password), IvParameterSpec(IV.toByteArray()))

            FileInputStream(source).use { input ->
                FileOutputStream(target).use { output ->
                    val total = source.length()
                    var readTotal = 0L
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    while (input.read(buf).also { read = it } != -1) {
                        val outBytes = cipher.update(buf, 0, read)
                        if (outBytes != null) output.write(outBytes)
                        readTotal += read
                        onProgress(readTotal.toFloat() / total)
                    }
                    val finalBytes = cipher.doFinal()
                    if (finalBytes != null) output.write(finalBytes)
                }
            }
            target
        }
    }
}