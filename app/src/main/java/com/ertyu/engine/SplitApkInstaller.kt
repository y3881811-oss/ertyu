package com.ertyu.engine

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile

object SplitApkInstaller {

    const val ACTION_INSTALL_RESULT = "com.ertyu.INSTALL_RESULT"
    private const val BUFFER_SIZE = 64 * 1024

    sealed class Result {
        object Success : Result()
        data class Failure(val message: String) : Result()
    }

    /**
     * 安装 .apks / .xapk / .apkm
     */
    suspend fun install(
        context: Context,
        archiveFile: File,
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> }
    ): Result = withContext(Dispatchers.IO) {
        var tempDir: File? = null
        try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!pm.canRequestPackageInstalls()) {
                    return@withContext Result.Failure("NEED_PERMISSION")
                }
            }

            onProgress("解析安装包...", 0, 100)
            tempDir = File(context.cacheDir, "split_install_${System.currentTimeMillis()}")
            if (!tempDir.exists()) tempDir.mkdirs()

            // 1. 解压所有内容
            ZipFile(archiveFile).use { zip ->
                val entries = zip.entries()
                var count = 0
                val total = zip.size()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val outFile = File(tempDir, entry.name)
                    if (!outFile.canonicalPath.startsWith(tempDir.canonicalPath)) {
                        continue // Zip Slip 防护
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output, BUFFER_SIZE)
                            }
                        }
                    }
                    count++
                    if (count % 10 == 0) onProgress("解压资源...", count, total)
                }
            }

            // 2. 查找所有 APK 和 OBB
            val apkFiles = mutableListOf<File>()
            val obbFiles = mutableListOf<File>()
            findFiles(tempDir, apkFiles, obbFiles)

            if (apkFiles.isEmpty()) {
                return@withContext Result.Failure("未在包内找到任何 APK 文件")
            }

            // 3. 提取 OBB 数据
            if (obbFiles.isNotEmpty()) {
                onProgress("释放 OBB 数据...", 0, 100)
                val obbRoot = File(Environment.getExternalStorageDirectory(), "Android/obb")
                // 获取包名 (通常从 base.apk 获取)
                val baseApk = apkFiles.firstOrNull { it.name.contains("base") } ?: apkFiles[0]
                val pkgName = pm.getPackageArchiveInfo(baseApk.absolutePath, 0)?.packageName
                
                if (pkgName != null) {
                    val targetObbDir = File(obbRoot, pkgName)
                    if (!targetObbDir.exists()) targetObbDir.mkdirs()
                    
                    obbFiles.forEach { obb ->
                        val dest = File(targetObbDir, obb.name)
                        obb.copyTo(dest, overwrite = true)
                    }
                }
            }

            // 4. Session 安装所有 APK
            onProgress("开始安装...", 0, apkFiles.size)
            val installer = pm.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(pm.getPackageArchiveInfo(apkFiles[0].absolutePath, 0)?.packageName)
            }
            val sessionId = installer.createSession(params)
            val session = installer.openSession(sessionId)

            try {
                apkFiles.forEachIndexed { index, apk ->
                    onProgress("写入分片...", index + 1, apkFiles.size)
                    val name = "split_${index}_${apk.name}"
                    session.openWrite(name, 0, apk.length()).use { out ->
                        FileInputStream(apk).use { input ->
                            input.copyTo(out, BUFFER_SIZE)
                        }
                        session.fsync(out)
                    }
                }

                val resultIntent = Intent(ACTION_INSTALL_RESULT).setPackage(context.packageName)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    sessionId,
                    resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                session.commit(pendingIntent.intentSender)
                Result.Success
            } finally {
                session.close()
            }

        } catch (e: Exception) {
            Result.Failure(e.message ?: "安装失败")
        } finally {
            // 清理临时目录
            tempDir?.deleteRecursively()
        }
    }

    private fun findFiles(dir: File, apks: MutableList<File>, obbs: MutableList<File>) {
        val files = dir.listFiles() ?: return
        for (f in files) {
            if (f.isDirectory) {
                findFiles(f, apks, obbs)
            } else {
                val ext = f.name.substringAfterLast('.', "").lowercase()
                if (ext == "apk") apks.add(f)
                else if (ext == "obb") obbs.add(f)
            }
        }
    }
}