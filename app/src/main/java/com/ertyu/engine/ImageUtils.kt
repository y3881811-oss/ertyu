package com.ertyu.engine

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

object ImageUtils {

    // 获取文件 MD5
    suspend fun getMd5(file: File): String? = withContext(Dispatchers.IO) {
        try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buf = ByteArray(8192)
                var read: Int
                while (input.read(buf).also { read = it } != -1) {
                    md.update(buf, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    // 压缩图片
    suspend fun compressImage(file: File, quality: Int = 80, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): File? = withContext(Dispatchers.IO) {
        try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return@withContext null
            val ext = if (format == Bitmap.CompressFormat.PNG) ".png" else ".jpg"
            val outFile = File(file.parent, file.nameWithoutExtension + "_compressed" + ext)
            FileOutputStream(outFile).use { out ->
                bitmap.compress(format, quality, out)
            }
            bitmap.recycle()
            outFile
        } catch (e: Exception) {
            null
        }
    }
}