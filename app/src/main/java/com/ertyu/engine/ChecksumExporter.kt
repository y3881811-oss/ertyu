package com.ertyu.engine

import android.os.Environment
import com.ertyu.model.ChecksumAlgorithm
import com.ertyu.model.ChecksumRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 把批量校验结果导出成 CSV（带 BOM，Excel/WPS 打开不乱码） */
object ChecksumExporter {

    suspend fun export(rows: List<ChecksumRow>, algorithm: ChecksumAlgorithm): File? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(Environment.getExternalStorageDirectory(), "Download")
                if (!dir.exists()) dir.mkdirs()
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(Date())
                val out = File(dir, "EYU-checksum-$stamp.csv")
                out.bufferedWriter().use { w ->
                    w.write("\uFEFF")
                    w.write("路径,文件名,大小(字节),算法,哈希,错误\n")
                    rows.forEach { r ->
                        w.write(csv(r.path) + ",")
                        w.write(csv(r.name) + ",")
                        w.write(r.size.toString() + ",")
                        w.write(algorithm.jca + ",")
                        w.write((r.hash ?: "") + ",")
                        w.write(csv(r.error ?: "") + "\n")
                    }
                }
                out
            } catch (_: Exception) {
                null
            }
        }

    private fun csv(s: String): String {
        val needQuote = s.any { it == ',' || it == '"' || it == '\n' }
        val escaped = s.replace("\"", "\"\"")
        return if (needQuote) "\"$escaped\"" else escaped
    }
}
