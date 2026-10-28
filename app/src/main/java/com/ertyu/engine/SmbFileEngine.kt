package com.ertyu.engine

import android.net.Uri
import com.ertyu.model.FileItem
import com.ertyu.model.SmbConfig
import com.ertyu.model.TransferProgress
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.EnumSet
import java.util.Locale

class SmbFileEngine(private val config: SmbConfig) : FileEngine {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private suspend fun <T> withShare(block: (DiskShare) -> T): T? = withContext(Dispatchers.IO) {
        try {
            val client = SMBClient()
            client.connect(config.host).use { connection ->
                val ac = AuthenticationContext(
                    config.username,
                    config.password.toCharArray(),
                    config.domain.ifBlank { null }
                )
                val session = connection.authenticate(ac)
                val share = session.connectShare(config.shareName) as? DiskShare
                share?.use { block(it) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun Uri.toSmbPath(): String {
        return path?.trimStart('/') ?: ""
    }

    override suspend fun list(uri: Uri): List<FileItem> {
        return withShare { share ->
            val path = uri.toSmbPath()
            val result = ArrayList<FileItem>()
            share.list(path).forEach { info ->
                val isDir = info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L
                val name = info.fileName
                val childPath = if (path.isEmpty()) name else "$path/$name"
                val fakeUri = Uri.parse("smb://${config.host}/${config.shareName}/$childPath")
                result.add(
                    FileItem(
                        name = name,
                        uri = fakeUri,
                        isDirectory = isDir,
                        size = info.endOfFile,
                        lastModified = info.changeTime?.toEpochMillis() ?: 0L,
                        uriString = fakeUri.toString(),
                        path = childPath,
                        sizeText = if (isDir) "" else formatSize(info.endOfFile),
                        typeText = if (isDir) "文件夹" else "文件",
                        lastModifiedText = info.changeTime?.let { dateFormat.format(Date(it.toEpochMillis())) } ?: "未知",
                        childCount = -1
                    )
                )
            }
            result.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
        } ?: emptyList()
    }

    override suspend fun delete(uri: Uri): Boolean = withShare { share ->
        val path = uri.toSmbPath()
        if (share.folderExists(path)) {
            share.rmdir(path, true)
        } else {
            share.rm(path)
        }
        true
    } ?: false

    override suspend fun createDirectory(parent: Uri, name: String): Uri? = withShare { share ->
        val path = parent.toSmbPath()
        val newPath = if (path.isEmpty()) name else "$path/$name"
        share.mkdir(newPath)
        Uri.parse("smb://${config.host}/${config.shareName}/$newPath")
    }

    override suspend fun rename(uri: Uri, newName: String): Uri? = withShare { share ->
        val path = uri.toSmbPath()
        val parentPath = path.substringBeforeLast('/', "")
        val newPath = if (parentPath.isEmpty()) newName else "$parentPath/$newName"
        share.rename(path, newPath)
        Uri.parse("smb://${config.host}/${config.shareName}/$newPath")
    }

    override suspend fun copy(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean = withShare { share ->
        val srcPath = source.toSmbPath()
        val dstPath = targetDir.toSmbPath()
        val fileName = srcPath.substringAfterLast('/')
        val finalDst = if (dstPath.isEmpty()) fileName else "$dstPath/$fileName"

        if (share.folderExists(srcPath)) {
            copyRecursively(share, srcPath, finalDst)
        } else {
            share.openFile(srcPath, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null).use { srcFile ->
                share.openFile(finalDst, EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null).use { dstFile ->
                    srcFile.inputStream.use { input ->
                        dstFile.outputStream.use { output ->
                            input.copyTo(output, 64 * 1024)
                        }
                    }
                }
            }
        }
        true
    } ?: false

    private fun copyRecursively(share: DiskShare, src: String, dst: String) {
        share.mkdir(dst)
        share.list(src).forEach { info ->
            val childSrc = "$src/${info.fileName}"
            val childDst = "$dst/${info.fileName}"
            if (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L) {
                copyRecursively(share, childSrc, childDst)
            } else {
                share.openFile(childSrc, EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null).use { sf ->
                    share.openFile(childDst, EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null).use { df ->
                        sf.inputStream.use { input -> df.outputStream.use { output -> input.copyTo(output) } }
                    }
                }
            }
        }
    }

    override suspend fun move(source: Uri, targetDir: Uri, onProgress: (TransferProgress) -> Unit): Boolean {
        val ok = copy(source, targetDir, onProgress)
        if (ok) delete(source)
        return ok
    }

    override fun getParent(uri: Uri): Uri? {
        val path = uri.toSmbPath()
        if (path.isEmpty()) return null
        val parent = path.substringBeforeLast('/', "")
        return Uri.parse("smb://${config.host}/${config.shareName}/$parent")
    }

    override fun getDisplayName(uri: Uri): String = uri.path?.substringAfterLast('/') ?: ""

    override suspend fun readText(uri: Uri): String? = withShare { share ->
        share.openFile(uri.toSmbPath(), EnumSet.of(AccessMask.GENERIC_READ), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null).use { file ->
            file.inputStream.bufferedReader().use { it.readText() }
        }
    }

    override suspend fun writeText(uri: Uri, content: String): Boolean = withShare { share ->
        share.openFile(uri.toSmbPath(), EnumSet.of(AccessMask.GENERIC_WRITE), null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF, null).use { file ->
            file.outputStream.bufferedWriter().use { it.write(content) }
        }
        true
    } ?: false

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.2f GB", gb)
    }
}