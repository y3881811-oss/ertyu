package com.ertyu.engine

import java.nio.ByteBuffer
import java.nio.ByteOrder

object AxmlDecoder {

    fun decode(axmlBytes: ByteArray): String {
        return try {
            val sb = StringBuilder()
            val buffer = ByteBuffer.wrap(axmlBytes).order(ByteOrder.LITTLE_ENDIAN)

            // AXML Header
            buffer.int // Magic 0x00080003
            buffer.int // File Size

            val stringPoolOffset = buffer.position()
            // StringPool Header
            val stringPoolType = buffer.short
            val stringPoolHeaderSize = buffer.short
            buffer.int // StringPool Size
            val stringCount = buffer.int
            buffer.int // Style Count
            val flags = buffer.int
            buffer.int // String Start
            buffer.int // Style Start

            val isUtf8 = (flags and (1 shl 8)) != 0
            val strings = mutableListOf<String>()
            val offsets = LongArray(stringCount)
            for (i in 0 until stringCount) {
                offsets[i] = buffer.int.toLong()
            }

            val stringPoolStart = stringPoolOffset + buffer.position()
            for (i in 0 until stringCount) {
                buffer.position((stringPoolStart + offsets[i]).toInt())
                if (isUtf8) {
                    readUtf8String(buffer)
                } else {
                    readUtf16String(buffer)
                }
                sb.clear()
                // 简单处理，跳过复杂的字符串池映射
            }

            // 简化的树形解析（只做基础遍历）
            // 真正的完整解析器需要映射资源 ID，这里做基础打印
            sb.clear()
            sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")

            // 重新定位到根节点开始解析
            val parser = AxmlParser(axmlBytes)
            parser.parse(sb)

            sb.toString()
        } catch (e: Exception) {
            "<!-- 解析 AXML 失败：${e.message} -->"
        }
    }

    private fun readUtf8String(buffer: ByteBuffer) {
        val len = buffer.get().toInt() and 0xFF
        var realLen = len
        if (len and 0x80 != 0) {
            realLen = ((len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF)
        }
        buffer.position(buffer.position() + realLen + 1)
    }

    private fun readUtf16String(buffer: ByteBuffer) {
        val len = buffer.short.toInt() and 0xFFFF
        buffer.position(buffer.position() + len * 2)
    }

    // 非常简化的 AXML 解析器，用于抽取节点名和属性
    private class AxmlParser(private val data: ByteArray) {
        private val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        private val stringPool = mutableListOf<String>()
        private var stringPoolStart = 0
        private var isUtf8 = false

        fun parse(sb: StringBuilder) {
            buffer.int; buffer.int // skip magic, size
            parseStringPool()
            buffer.int // skip
            parseNodes(sb)
        }

        private fun parseStringPool() {
            val chunkStart = buffer.position()
            buffer.short; buffer.short; buffer.int // skip headers
            val count = buffer.int
            buffer.int; val flags = buffer.int
            buffer.int; buffer.int
            isUtf8 = (flags and (1 shl 8)) != 0
            val offsets = LongArray(count) { buffer.int.toLong() }
            stringPoolStart = chunkStart + buffer.position()
            val end = chunkStart + buffer.position() + count * 4 // rough

            for (i in 0 until count) {
                buffer.position((stringPoolStart + offsets[i]).toInt())
                if (isUtf8) {
                    val len = buffer.get().toInt() and 0xFF
                    val realLen = if (len and 0x80 != 0) ((len and 0x7F) shl 8) or (buffer.get().toInt() and 0xFF) else len
                    buffer.get() // utf-8 len 跳过
                    val bytes = ByteArray(realLen)
                    buffer.get(bytes)
                    stringPool.add(String(bytes, Charsets.UTF_8))
                } else {
                    val len = buffer.short.toInt()
                    val bytes = ByteArray(len * 2)
                    buffer.get(bytes)
                    stringPool.add(String(bytes, Charsets.UTF_16LE))
                }
            }
            buffer.position(end)
        }

        private fun parseNodes(sb: StringBuilder) {
            var indent = 0
            while (buffer.remaining() > 8) {
                val type = buffer.short.toInt()
                val headerSize = buffer.short.toInt()
                val chunkSize = buffer.int

                if (type == 0x0102) { // START_TAG
                    val ext = buffer.position() - 8 + headerSize
                    buffer.int // line
                    buffer.int // comment
                    val ns = buffer.int
                    val name = buffer.int
                    val attrStart = buffer.short
                    val attrSize = buffer.short
                    val attrCount = buffer.short
                    buffer.short // id, class, style

                    repeat(indent) { sb.append("  ") }
                    sb.append("<${stringPool[name]}")
                    
                    val attrsStart = buffer.position()
                    buffer.position(ext + attrStart) // 跳到属性区

                    for (i in 0 until attrCount) {
                        val nsIdx = buffer.int
                        val nameIdx = buffer.int
                        val rawIdx = buffer.int
                        buffer.short; buffer.get() // size, res0
                        val type = buffer.get()
                        val valData = buffer.int
                        
                        val attrName = stringPool[nameIdx]
                        val attrVal = if (rawIdx != -1) stringPool[rawIdx] else {
                            when (type) {
                                0x10 -> valData.toString() // int
                                0x12 -> if (valData != 0) "true" else "false"
                                else -> "0x${Integer.toHexString(valData)}"
                            }
                        }
                        sb.append("\n")
                        repeat(indent + 1) { sb.append("  ") }
                        sb.append("$attrName=\"$attrVal\"")
                    }
                    sb.append(">\n")
                    indent++
                    buffer.position(ext + chunkSize)

                } else if (type == 0x0103) { // END_TAG
                    indent = (indent - 1).coerceAtLeast(0)
                    val name = buffer.int // simplified
                    repeat(indent) { sb.append("  ") }
                    sb.append("</${stringPool[name]}>\n")
                    buffer.position(buffer.position() + chunkSize - 8)
                } else {
                    buffer.position(buffer.position() + chunkSize - 8)
                }
            }
        }
    }
}