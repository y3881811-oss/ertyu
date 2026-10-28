package com.ertyu.engine

import com.android.apksig.ApkSigner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate

object ApkSignerEngine {

    suspend fun sign(
        inputApk: File,
        outputApk: File,
        keystoreFile: File,
        storePass: String,
        keyAlias: String,
        keyPass: String,
        onProgress: (String) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("加载 keystore…")
            val ks = KeyStore.getInstance("JKS")
            FileInputStream(keystoreFile).use { input ->
                ks.load(input, storePass.toCharArray())
            }

            val key = ks.getKey(keyAlias, keyPass.toCharArray())
                ?: throw IllegalArgumentException("找不到别名：$keyAlias")
            val cert = ks.getCertificate(keyAlias)
                ?: throw IllegalArgumentException("找不到证书：$keyAlias")

            if (key !is PrivateKey) {
                throw IllegalArgumentException("别名对应的不是私钥")
            }
            if (cert !is X509Certificate) {
                throw IllegalArgumentException("证书不是 X509 格式")
            }

            onProgress("准备签名…")
            val signerConfig = ApkSigner.SignerConfig.Builder(
                keyAlias,
                key,
                listOf(cert)
            ).build()

            if (outputApk.exists()) outputApk.delete()

            onProgress("签名中…")
            ApkSigner.Builder(listOf(signerConfig))
                .setInputApk(inputApk)
                .setOutputApk(outputApk)
                .setV1SigningEnabled(true)
                .setV2SigningEnabled(true)
                .build()
                .sign()

            onProgress("签名完成")
        }
    }
}