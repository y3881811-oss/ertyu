package com.ertyu.engine

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.ertyu.model.ApkSignature
import com.ertyu.model.SignerCertificate
import java.io.ByteArrayInputStream
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipFile

object ApkSignatureInspector {

    fun inspect(context: Context, apkFile: File): Result<ApkSignature> = runCatching {
        val pm = context.packageManager

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val packageInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: throw IllegalArgumentException("无法解析 APK")

        val signers = mutableListOf<SignerCertificate>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            if (signingInfo != null) {
                val sigs = signingInfo.apkContentsSigners
                    ?: signingInfo.signingCertificateHistory
                if (sigs != null) {
                    for (sig in sigs) {
                        runCatching {
                            signers.add(parseCertificate(sig.toByteArray()))
                        }
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            val sigs = packageInfo.signatures
            if (sigs != null) {
                for (sig in sigs) {
                    runCatching {
                        signers.add(parseCertificate(sig.toByteArray()))
                    }
                }
            }
        }

        val hasV1 = hasV1Signature(apkFile)
        val blockIds = readSigningBlockIds(apkFile)
        val hasV2 = blockIds.contains(0x7109871aL)
        val hasV3 = blockIds.contains(0xf05368c0L)
        val hasV31 = blockIds.contains(0x1b93ad61L)

        ApkSignature(
            signers = signers,
            v1 = hasV1,
            v2 = hasV2,
            v3 = hasV3,
            v31 = hasV31
        )
    }

    private fun parseCertificate(bytes: ByteArray): SignerCertificate {
        val factory = CertificateFactory.getInstance("X.509")
        val cert = factory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        return SignerCertificate(
            subject = cert.subjectDN.name ?: "—",
            issuer = cert.issuerDN.name ?: "—",
            serialNumber = cert.serialNumber.toString(16),
            validFrom = dateFormat.format(cert.notBefore),
            validTo = dateFormat.format(cert.notAfter),
            md5 = fingerprint(cert.encoded, "MD5"),
            sha1 = fingerprint(cert.encoded, "SHA-1"),
            sha256 = fingerprint(cert.encoded, "SHA-256")
        )
    }

    private fun fingerprint(bytes: ByteArray, algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        val digest = md.digest(bytes)
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun hasV1Signature(apkFile: File): Boolean {
        return try {
            ZipFile(apkFile).use { zip ->
                zip.entries().asSequence().any { entry ->
                    val name = entry.name
                    name.startsWith("META-INF/") && name.endsWith(".SF")
                }
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun readSigningBlockIds(apkFile: File): Set<Long> {
        return try {
            RandomAccessFile(apkFile, "r").use { raf ->
                val fileLength = raf.length()
                if (fileLength < 32) return emptySet()

                val maxSearch = minOf(fileLength, 22L + 65535L).toInt()
                val buffer = ByteArray(maxSearch)
                raf.seek(fileLength - maxSearch)
                raf.readFully(buffer)

                var eocdOffset = -1
                for (i in buffer.size - 22 downTo 0) {
                    if (buffer[i] == 0x50.toByte() &&
                        buffer[i + 1] == 0x4b.toByte() &&
                        buffer[i + 2] == 0x05.toByte() &&
                        buffer[i + 3] == 0x06.toByte()
                    ) {
                        eocdOffset = i
                        break
                    }
                }
                if (eocdOffset == -1) return emptySet()

                val cdOffset = readIntLE(buffer, eocdOffset + 16).toLong() and 0xFFFFFFFFL
                if (cdOffset < 32) return emptySet()

                val tail = ByteArray(16)
                raf.seek(cdOffset - 16)
                raf.readFully(tail)
                val magic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)
                if (!tail.contentEquals(magic)) return emptySet()

                val sizeBytes = ByteArray(8)
                raf.seek(cdOffset - 24)
                raf.readFully(sizeBytes)
                val blockSize = readLongLE(sizeBytes)
                if (blockSize < 24 || blockSize > cdOffset - 8) return emptySet()

                val blockStart = cdOffset - 8 - blockSize
                if (blockStart < 0) return emptySet()

                val pairsLen = (blockSize - 8).toInt()
                if (pairsLen <= 0) return emptySet()
                val pairsBuffer = ByteArray(pairsLen)
                raf.seek(blockStart + 8)
                raf.readFully(pairsBuffer)

                val ids = mutableSetOf<Long>()
                var pos = 0
                while (pos + 8 <= pairsBuffer.size) {
                    val pairLen = (readIntLE(pairsBuffer, pos).toLong() and 0xFFFFFFFFL)
                    if (pairLen < 4 || pos + 8 + pairLen > pairsBuffer.size) break
                    val id = readIntLE(pairsBuffer, pos + 4).toLong() and 0xFFFFFFFFL
                    ids.add(id)
                    pos += 8 + pairLen.toInt()
                }
                ids
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    private fun readIntLE(buf: ByteArray, off: Int): Int {
        return (buf[off].toInt() and 0xFF) or
                ((buf[off + 1].toInt() and 0xFF) shl 8) or
                ((buf[off + 2].toInt() and 0xFF) shl 16) or
                ((buf[off + 3].toInt() and 0xFF) shl 24)
    }

    private fun readLongLE(buf: ByteArray): Long {
        var result = 0L
        for (i in 0 until 8) {
            result = result or ((buf[i].toLong() and 0xFF) shl (i * 8))
        }
        return result
    }
}