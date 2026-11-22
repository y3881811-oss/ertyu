package com.ertyu.engine

import android.content.Context
import android.content.pm.PackageManager
import com.android.apksig.ApkVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** 更新包校验：SHA256 + 签名必须与当前安装的 App 一致 */
object UpdateVerifier {

    fun sha256Of(file: File): String? = try {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                md.update(buf, 0, n)
            }
        }
        md.digest().joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }

    /** 解析 SHA256SUMS.txt：每行 "hash  filename" 或 "hash *filename" */
    fun parseSums(text: String): Map<String, String> {
        val map = HashMap<String, String>()
        text.lineSequence().forEach { line ->
            val t = line.trim()
            if (t.isEmpty()) return@forEach
            val parts = t.split(Regex("\\s+"), limit = 2)
            if (parts.size < 2) return@forEach
            val hash = parts[0].lowercase()
            val name = parts[1].trim().removePrefix("*").substringAfterLast('/')
            if (hash.length == 64) map[name] = hash
        }
        return map
    }

    /**
     * 校验 APK 签名是否与当前 App 一致。
     * 返回 (是否通过, 说明)。签名一致才允许安装，能挡住代理替换/二次打包。
     */
    suspend fun verifySignature(context: Context, apkFile: File): Pair<Boolean, String> =
        withContext(Dispatchers.IO) {
            try {
                if (!apkFile.isFile || apkFile.length() <= 0) return@withContext false to "文件不存在或为空"

                val result = ApkVerifier.Builder(apkFile).build().verify()
                if (!result.isVerified) {
                    val err = result.errors.firstOrNull()?.toString() ?: "未通过 apksig 校验"
                    return@withContext false to "APK 签名无效：$err"
                }
                val newCerts = result.signerCertificates.mapNotNull { sha256Hex(it.encoded) }.toSet()
                if (newCerts.isEmpty()) return@withContext false to "APK 没有签名证书"

                @Suppress("DEPRECATION")
                val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                val current = info.signatures?.mapNotNull { sha256Hex(it.toByteArray()) }?.toSet() ?: emptySet()
                if (current.isEmpty()) return@withContext false to "读不到当前 App 的签名"

                return@withContext if (newCerts.any { it in current }) {
                    true to "签名与当前 App 一致"
                } else {
                    false to "签名与当前 App 不一致（可能是被替换或换签名重打包）"
                }
            } catch (t: Throwable) {
                AppLogger.e("UpdateVerify", "签名校验异常", t)
                false to (t.message ?: "签名校验异常")
            }
        }

    private fun sha256Hex(bytes: ByteArray): String? = try {
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    } catch (_: Exception) {
        null
    }
}
