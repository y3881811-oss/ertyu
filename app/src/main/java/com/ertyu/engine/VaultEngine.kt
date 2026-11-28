package com.ertyu.engine

import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class VaultEntry(
    val id: String,
    val name: String,
    val isDir: Boolean,
    val size: Long,
    val mtime: Long,
    val blob: String
)

/**
 * 隐私空间：文件用 AES-256-GCM 加密后放在 .EYU_Vault 目录，
 * 密码用 PBKDF2 派生，只保存 salt + 校验值，**忘记密码无法恢复**。
 */
object VaultEngine {

    private const val MAGIC = "EYUV1"
    private const val ITER = 120_000
    private const val SALT_LEN = 16
    private const val IV_LEN = 12
    private const val TAG_BITS = 128
    private const val META = "vault.json"

    val dir: File get() = File(Environment.getExternalStorageDirectory(), ".EYU_Vault")
    private val metaFile: File get() = File(dir, META)

    fun exists(): Boolean = metaFile.isFile

    private fun readMeta(): JSONObject? = try {
        JSONObject(metaFile.readText())
    } catch (_: Exception) {
        null
    }

    private fun writeMeta(o: JSONObject) {
        dir.mkdirs()
        metaFile.writeText(o.toString())
    }

    private fun keyOf(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITER, 256)
        val f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return f.generateSecret(spec).encoded
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    fun hasPassword(): Boolean = exists()

    fun setPassword(password: String): Boolean {
        if (password.length < 4) return false
        return try {
            dir.mkdirs()
            val salt = ByteArray(SALT_LEN).also { SecureRandom().nextBytes(it) }
            val key = keyOf(password, salt)
            val meta = JSONObject()
            meta.put("v", 1)
            meta.put("salt", android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP))
            meta.put("verify", sha256(key))
            meta.put("entries", JSONArray())
            writeMeta(meta)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun verify(password: String): Boolean {
        val meta = readMeta() ?: return false
        return try {
            val salt = android.util.Base64.decode(meta.getString("salt"), android.util.Base64.NO_WRAP)
            sha256(keyOf(password, salt)) == meta.getString("verify")
        } catch (_: Exception) {
            false
        }
    }

    private fun keyFor(password: String): ByteArray? {
        val meta = readMeta() ?: return null
        return try {
            val salt = android.util.Base64.decode(meta.getString("salt"), android.util.Base64.NO_WRAP)
            val key = keyOf(password, salt)
            if (sha256(key) != meta.getString("verify")) null else key
        } catch (_: Exception) {
            null
        }
    }

    fun list(): List<VaultEntry> {
        val meta = readMeta() ?: return emptyList()
        val arr = meta.optJSONArray("entries") ?: return emptyList()
        val out = ArrayList<VaultEntry>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            out.add(
                VaultEntry(
                    id = o.optString("id"),
                    name = o.optString("name"),
                    isDir = o.optBoolean("isDir"),
                    size = o.optLong("size"),
                    mtime = o.optLong("mtime"),
                    blob = o.optString("blob")
                )
            )
        }
        return out.sortedByDescending { it.mtime }
    }

    private fun saveEntries(entries: List<VaultEntry>) {
        val meta = readMeta() ?: return
        val arr = JSONArray()
        entries.forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("name", it.name)
                    .put("isDir", it.isDir)
                    .put("size", it.size)
                    .put("mtime", it.mtime)
                    .put("blob", it.blob)
            )
        }
        meta.put("entries", arr)
        writeMeta(meta)
    }

    private fun encryptTo(src: File, dst: File, key: ByteArray, onProgress: (Float) -> Unit) {
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        dst.outputStream().buffered().use { out ->
            out.write(MAGIC.toByteArray())
            out.write(iv)
            val total = src.length().coerceAtLeast(1)
            var done = 0L
            src.inputStream().buffered().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    val enc = cipher.update(buf, 0, n)
                    if (enc != null && enc.isNotEmpty()) out.write(enc)
                    done += n
                    onProgress(done.toFloat() / total)
                }
            }
            val tail = cipher.doFinal()
            if (tail.isNotEmpty()) out.write(tail)
        }
    }

    private fun decryptTo(src: File, dst: File, key: ByteArray, onProgress: (Float) -> Unit) {
        src.inputStream().buffered().use { input ->
            val magic = ByteArray(MAGIC.length)
            if (input.read(magic) != magic.size || String(magic) != MAGIC) {
                throw IllegalStateException("不是有效的隐私空间文件")
            }
            val iv = ByteArray(IV_LEN)
            if (input.read(iv) != iv.size) throw IllegalStateException("文件已损坏")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
            dst.parentFile?.mkdirs()
            dst.outputStream().buffered().use { out ->
                val total = src.length().coerceAtLeast(1)
                var done = 0L
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    val dec = cipher.update(buf, 0, n)
                    if (dec != null && dec.isNotEmpty()) out.write(dec)
                    done += n
                    onProgress(done.toFloat() / total)
                }
                val tail = cipher.doFinal()
                if (tail.isNotEmpty()) out.write(tail)
            }
        }
    }

    /** 把文件/文件夹移入隐私空间（原件删除），返回条目 */
    suspend fun importEntry(
        password: String,
        source: File,
        onProgress: (Float) -> Unit = {}
    ): Result<VaultEntry> = withContext(Dispatchers.IO) {
        runCatching {
            val key = keyFor(password) ?: throw IllegalStateException("密码错误")
            dir.mkdirs()
            val id = System.currentTimeMillis().toString(36) + "_" + (1000..9999).random()
            val isDir = source.isDirectory

            val payload: File = if (isDir) {
                // 目录先打成临时 zip
                val tmp = File(dir, ".tmp_$id.zip")
                val r = ZipEngine.compress(listOf(source), tmp) { a, b ->
                    if (b > 0) onProgress(a.toFloat() / b)
                }
                r.getOrThrow()
                tmp
            } else {
                source
            }

            val blobName = "$id.bin"
            val blob = File(dir, blobName)
            encryptTo(payload, blob, key, onProgress)
            if (isDir) payload.delete()

            val entry = VaultEntry(
                id = id,
                name = source.name,
                isDir = isDir,
                size = blob.length(),
                mtime = System.currentTimeMillis(),
                blob = blobName
            )
            saveEntries(list() + entry)
            // 原件删除（目录递归）
            if (source.isDirectory) source.deleteRecursively() else source.delete()
            entry
        }
    }

    /** 解密导出到目标目录 */
    suspend fun exportEntry(
        password: String,
        entry: VaultEntry,
        targetDir: File,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val key = keyFor(password) ?: throw IllegalStateException("密码错误")
            val blob = File(dir, entry.blob)
            if (!blob.isFile) throw IllegalStateException("文件丢失：${entry.name}")
            if (entry.isDir) {
                val tmpZip = File(dir, ".tmp_${entry.id}.zip")
                decryptTo(blob, tmpZip, key, onProgress)
                val dest = File(targetDir, entry.name)
                dest.mkdirs()
                val r = ArchiveEngine.extract(tmpZip, dest)
                tmpZip.delete()
                r.getOrThrow()
                dest
            } else {
                var name = entry.name
                var dest = File(targetDir, name)
                var i = 1
                while (dest.exists()) {
                    val base = name.substringBeforeLast('.', name)
                    val ext = if (name.contains('.')) "." + name.substringAfterLast('.') else ""
                    dest = File(targetDir, "$base (${i++})$ext")
                }
                decryptTo(blob, dest, key, onProgress)
                dest
            }
        }
    }

    fun deleteEntry(entry: VaultEntry) {
        try {
            File(dir, entry.blob).delete()
            saveEntries(list().filterNot { it.id == entry.id })
        } catch (_: Exception) {
        }
    }

    /** 删除整个隐私空间（不可恢复） */
    fun destroy() {
        dir.deleteRecursively()
    }
}
