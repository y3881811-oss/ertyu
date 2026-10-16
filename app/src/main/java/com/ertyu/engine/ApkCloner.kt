package com.ertyu.engine

import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ApkCloner {

    /**
     * APK 共存/克隆：
     * 1. 解压出 AndroidManifest.xml
     * 2. 用字节替换法修改包名（新包名长度必须与原包名相等）
     * 3. 重新打包
     * 4. 用 keystore 重新签名
     */
    suspend fun clone(
        apkFile: File,
        outputApk: File,
        oldPackage: String,
        newPackage: String,
        keystoreFile: File,
        storePass: String,
        keyAlias: String,
        keyPass: String,
        onProgress: (String) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (newPackage.length != oldPackage.length) {
                throw IllegalArgumentException(
                    "新包名长度必须与原包名相同" +
                            "（原 ${oldPackage.length} 字符，" +
                            "新 ${newPackage.length} 字符）"
                )
            }
            if (newPackage == oldPackage) {
                throw IllegalArgumentException("新包名不能与原包名相同")
            }

            onProgress("读取原 APK…")

            val oldUtf8 = oldPackage.toByteArray(Charsets.UTF_8)
            val newUtf8 = newPackage.toByteArray(Charsets.UTF_8)
            val oldUtf16 = oldPackage.toByteArray(Charsets.UTF_16LE)
            val newUtf16 = newPackage.toByteArray(Charsets.UTF_16LE)

            // 1. 修改 AndroidManifest.xml
            var newManifestBytes: ByteArray
            ZipFile(apkFile).use { zip ->
                val entry = zip.getEntry("AndroidManifest.xml")
                    ?: throw IllegalStateException(
                        "APK 里找不到 AndroidManifest.xml"
                    )
                zip.getInputStream(entry).use { input ->
                    val bytes = input.readBytes()
                    var result = replaceBytes(bytes, oldUtf8, newUtf8)
                    if (result.contentEquals(bytes)) {
                        // UTF-8 没找到，试 UTF-16LE
                        result = replaceBytes(bytes, oldUtf16, newUtf16)
                    }
                    if (result.contentEquals(bytes)) {
                        throw IllegalStateException("在 AndroidManifest 里找不到原包名")
                    }
                    newManifestBytes = result
                }
            }

            onProgress("重新打包…")
            val tempApk = File(outputApk.parentFile, "_clone_tmp.apk")
            if (tempApk.exists()) tempApk.delete()

            ZipFile(apkFile).use { zip ->
                ZipOutputStream(tempApk.outputStream().buffered()).use { zos ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name

                        // 跳过旧签名
                        if (name.startsWith("META-INF/") &&
                            (name.endsWith(".SF") ||
                                    name.endsWith(".RSA") ||
                                    name.endsWith(".DSA") ||
                                    name.endsWith(".EC") ||
                                    name == "META-INF/MANIFEST.MF")
                        ) {
                            continue
                        }

                        val newEntry = ZipEntry(name)
                        newEntry.time = entry.time
                        zos.putNextEntry(newEntry)

                        if (name == "AndroidManifest.xml") {
                            zos.write(newManifestBytes)
                        } else {
                            zip.getInputStream(entry).use { it.copyTo(zos) }
                        }
                        zos.closeEntry()
                    }
                }
            }

            // 2. 签名
            onProgress("签名…")
            val ks = KeyStore.getInstance("JKS")
            FileInputStream(keystoreFile).use {
                ks.load(it, storePass.toCharArray())
            }
            val key = ks.getKey(keyAlias, keyPass.toCharArray()) as PrivateKey
            val cert = ks.getCertificate(keyAlias) as X509Certificate
            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias, key, listOf(cert)
            ).build()

            if (outputApk.exists()) outputApk.delete()
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(tempApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign()

            tempApk.delete()
            onProgress("完成")
        }
    }

    private fun replaceBytes(
        haystack: ByteArray,
        needle: ByteArray,
        replacement: ByteArray
    ): ByteArray {
        if (needle.isEmpty() || needle.size != replacement.size) {
            return haystack
        }
        val result = haystack.copyOf()
        var i = 0
        var found = false
        while (i <= result.size - needle.size) {
            var match = true
            for (j in needle.indices) {
                if (result[i + j] != needle[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                for (j in replacement.indices) {
                    result[i + j] = replacement[j]
                }
                i += needle.size
                found = true
            } else {
                i++
            }
        }
        return if (found) result else haystack
    }
}