package com.ertyu.engine

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

object FeatureEngines {

    // ================== 1. 代码语法高亮 ==================
    private val keywordColor = Color(0xFFCC7832)
    private val stringColor = Color(0xFF6A8759)
    private val commentColor = Color(0xFF808080)
    private val numberColor = Color(0xFF6897BB)

    fun highlight(code: String, extension: String): AnnotatedString {
        return buildAnnotatedString {
            append(code)
            val keywords = when (extension.lowercase()) {
                "kt", "java" -> listOf("fun", "val", "var", "class", "object", "if", "else", "return", "public", "private", "import", "package")
                "xml" -> listOf("xml", "version", "encoding", "manifest", "application", "activity")
                "smali" -> listOf("class", "super", "method", "invoke", "return", "const")
                "json" -> listOf("true", "false", "null")
                else -> emptyList()
            }
            keywords.forEach { keyword ->
                Regex("\\b$keyword\\b").findAll(code).forEach { match ->
                    addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), match.range.first, match.range.last + 1)
                }
            }
            Regex("\".*?\"").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = stringColor), match.range.first, match.range.last + 1)
            }
            Regex("(//.*|#.*)").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = commentColor), match.range.first, match.range.last + 1)
            }
            Regex("\\b\\d+\\b").findAll(code).forEach { match ->
                addStyle(SpanStyle(color = numberColor), match.range.first, match.range.last + 1)
            }
        }
    }

    // ================== 2. 全局内容搜索 ==================
    fun searchContent(rootDir: File, keyword: String): Flow<Pair<File, Pair<Int, String>>> = flow {
        val regex = Regex(Regex.escape(keyword), RegexOption.IGNORE_CASE)
        val ignoreExts = listOf("apk", "zip", "jpg", "png", "mp4", "mp3", "so", "dex", "jar", "ttf", "db")

        rootDir.walkTopDown().forEach { file ->
            // 👇 修复：在 flow 中正确使用 ensureActive
            currentCoroutineContext().ensureActive()
            
            if (file.isFile && file.length() < 5 * 1024 * 1024 && file.extension.lowercase() !in ignoreExts) {
                try {
                    file.useLines { lines ->
                        lines.forEachIndexed { index, line ->
                            if (regex.containsMatchIn(line)) {
                                emit(file to (index + 1 to line.trim()))
                            }
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }.flowOn(Dispatchers.IO)

    // ================== 3. EXIF 读取与重命名 ==================
    data class ExifData(val make: String?, val model: String?, val dateTime: String?, val gps: String?)

    fun getExif(file: File): ExifData? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val latLong = exif.latLong
            ExifData(
                make = exif.getAttribute(ExifInterface.TAG_MAKE),
                model = exif.getAttribute(ExifInterface.TAG_MODEL),
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                gps = if (latLong != null) "%.4f, %.4f".format(latLong[0], latLong[1]) else null
            )
        } catch (e: Exception) { null }
    }

    fun generateExifRename(file: File): String? {
        val exif = getExif(file) ?: return null
        val dateStr = exif.dateTime ?: return null
        return try {
            val parser = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
            val date = parser.parse(dateStr) ?: return null
            val formatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            "${formatter.format(date)}_${file.nameWithoutExtension.takeLast(4)}.${file.extension}"
        } catch (e: Exception) { null }
    }

    // ================== 4. 重复文件扫描 ==================
    fun scanDuplicates(rootDir: File): Flow<Pair<String, List<File>>> = flow {
        val fileSizeMap = mutableMapOf<Long, MutableList<File>>()
        rootDir.walkTopDown().forEach { file ->
            // 👇 修复：在 flow 中正确使用 ensureActive
            currentCoroutineContext().ensureActive()
            
            if (file.isFile && file.length() > 0) {
                fileSizeMap.getOrPut(file.length()) { mutableListOf() }.add(file)
            }
        }

        for ((_, files) in fileSizeMap) {
            if (files.size > 1) {
                val hashMap = mutableMapOf<String, MutableList<File>>()
                for (file in files) {
                    // 👇 修复：在 flow 中正确使用 ensureActive
                    currentCoroutineContext().ensureActive()
                    
                    val md5 = getMD5(file)
                    if (md5 != null) hashMap.getOrPut(md5) { mutableListOf() }.add(file)
                }
                for ((hash, duplicateFiles) in hashMap) {
                    if (duplicateFiles.size > 1) {
                        emit(hash to duplicateFiles)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun getMD5(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } > 0) md.update(buffer, 0, read)
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) { null }
    }
}