package com.ertyu.ui

import androidx.compose.ui.graphics.Color

/** 主题强调色候选（只换 primary，其余沿用 Material 3 默认配色） */
object AccentPalette {

    val names = listOf("经典蓝", "青绿", "紫罗兰", "琥珀", "玫红", "竹绿")

    private val values = listOf(
        0xFF1E88E5L,
        0xFF00BFA5L,
        0xFF7C4DFFL,
        0xFFFFA000L,
        0xFFEC407AL,
        0xFF43A047L
    )

    fun colorOf(index: Int): Color = Color(values.getOrElse(index) { values[0] })

    fun indexOf(color: Color): Int {
        val v = color.value.toLong()
        return values.indexOfFirst { it == v }.let { if (it < 0) 0 else it }
    }
}
