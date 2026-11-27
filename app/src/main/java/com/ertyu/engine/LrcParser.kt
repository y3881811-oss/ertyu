package com.ertyu.engine

import java.io.File

data class LrcLine(val timeMs: Long, val text: String)

/** 解析同名 .lrc 歌词（支持一行多个时间标签） */
object LrcParser {

    private val timeTag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")

    fun load(audioPath: String): List<LrcLine> {
        if (audioPath.isBlank()) return emptyList()
        val audio = File(audioPath)
        val base = audio.name.substringBeforeLast('.', audio.name)
        val dir = audio.parentFile ?: return emptyList()
        val candidates = listOf("$base.lrc", "$base.LRC", "$base.Lrc")
        for (name in candidates) {
            val f = File(dir, name)
            if (f.isFile) {
                val text = try {
                    TextCodec.read(f)
                } catch (_: Exception) {
                    null
                } ?: continue
                val lines = parse(text)
                if (lines.isNotEmpty()) return lines
            }
        }
        return emptyList()
    }

    fun parse(text: String): List<LrcLine> {
        val out = ArrayList<LrcLine>()
        text.lineSequence().forEach { raw ->
            val tags = timeTag.findAll(raw).toList()
            if (tags.isEmpty()) return@forEach
            val content = raw.substring(tags.last().range.last + 1).trim()
            if (content.isEmpty()) return@forEach
            tags.forEach { m ->
                val min = m.groupValues[1].toLongOrNull() ?: 0L
                val sec = m.groupValues[2].toLongOrNull() ?: 0L
                val fracRaw = m.groupValues[3]
                val frac = when (fracRaw.length) {
                    0 -> 0L
                    1 -> fracRaw.toLongOrNull()?.times(100) ?: 0L
                    2 -> fracRaw.toLongOrNull()?.times(10) ?: 0L
                    else -> fracRaw.take(3).toLongOrNull() ?: 0L
                }
                out.add(LrcLine(min * 60_000L + sec * 1000L + frac, content))
            }
        }
        return out.sortedBy { it.timeMs }
    }
}
