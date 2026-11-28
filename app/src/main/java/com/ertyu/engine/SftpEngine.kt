package com.ertyu.engine

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.util.Properties

data class RemoteEntry(
    val name: String,
    val path: String,
    val isDir: Boolean,
    val size: Long,
    val modified: Long
)

/** 一次 SFTP 连接。ChannelSftp 不是线程安全的，所有操作都用 mutex 串行化。 */
class SftpSession(
    val host: String,
    val port: Int,
    val user: String,
    internal val session: Session,
    internal val channel: ChannelSftp
) {
    internal val mutex = Mutex()

    fun close() {
        runCatching { channel.disconnect() }
        runCatching { session.disconnect() }
    }
}

object SftpEngine {

    private const val TIMEOUT_MS = 15000

    suspend fun connect(
        host: String,
        port: Int,
        user: String,
        password: String,
        privateKeyPath: String? = null,
        keyPassphrase: String? = null
    ): Result<SftpSession> =
        withContext(Dispatchers.IO) {
            runCatching {
                val jsch = JSch()
                val keyPath = privateKeyPath?.trim().orEmpty()
                val useKey = keyPath.isNotEmpty()
                if (useKey) {
                    val kf = java.io.File(keyPath)
                    if (!kf.isFile) throw IllegalArgumentException("私钥文件不存在：$keyPath")
                    if (keyPassphrase.isNullOrEmpty()) {
                        jsch.addIdentity(kf.absolutePath)
                    } else {
                        jsch.addIdentity(kf.absolutePath, keyPassphrase)
                    }
                }
                val session = jsch.getSession(user, host, port)
                if (password.isNotEmpty()) session.setPassword(password)
                val cfg = Properties()
                // 首次连接不校验主机指纹（与多数手机 SFTP 客户端一致），凭据仍走加密通道
                cfg["StrictHostKeyChecking"] = "no"
                cfg["PreferredAuthentications"] =
                    if (useKey) "publickey,keyboard-interactive,password" else "password,keyboard-interactive"
                session.setConfig(cfg)
                session.connect(TIMEOUT_MS)

                val channel = session.openChannel("sftp") as ChannelSftp
                channel.connect(TIMEOUT_MS)
                SftpSession(host, port, user, session, channel)
            }
        }

    suspend fun list(s: SftpSession, path: String): Result<List<RemoteEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            s.mutex.withLock {
                val items = s.channel.ls(path)
                val list = ArrayList<RemoteEntry>(items.size)
                for (o in items) {
                    val e = o as ChannelSftp.LsEntry
                    val name = e.filename
                    if (name == "." || name == "..") continue
                    val full = if (path.endsWith("/")) "$path$name" else "$path/$name"
                    list.add(
                        RemoteEntry(
                            name = name,
                            path = full,
                            isDir = e.attrs.isDir,
                            size = e.attrs.size,
                            modified = e.attrs.mTime.toLong() * 1000L
                        )
                    )
                }
                list.sortWith(compareBy({ !it.isDir }, { it.name.lowercase() }))
                list
            }
        }
    }

    suspend fun download(
        s: SftpSession,
        remotePath: String,
        localFile: File,
        onProgress: (Long) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            s.mutex.withLock {
                localFile.parentFile?.mkdirs()
                s.channel.get(remotePath).use { input ->
                    BufferedInputStream(input).use { bin ->
                        BufferedOutputStream(localFile.outputStream()).use { out ->
                            val buf = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                val n = bin.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                total += n
                                onProgress(total)
                            }
                        }
                    }
                }
            }
        }
    }

    suspend fun upload(
        s: SftpSession,
        localFile: File,
        remotePath: String,
        onProgress: (Long) -> Unit = {}
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            s.mutex.withLock {
                BufferedInputStream(localFile.inputStream()).use { bin ->
                    s.channel.put(bin, remotePath, ChannelSftp.OVERWRITE)
                }
                onProgress(localFile.length())
            }
        }
    }

    suspend fun mkdir(s: SftpSession, path: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching { s.mutex.withLock { s.channel.mkdir(path) } }
    }

    suspend fun delete(s: SftpSession, path: String, isDir: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            s.mutex.withLock {
                if (isDir) s.channel.rmdir(path) else s.channel.rm(path)
            }
        }
    }
}
