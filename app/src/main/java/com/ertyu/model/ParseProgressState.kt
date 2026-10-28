package com.ertyu.model

data class ParseProgressState(
    val visible: Boolean = false,
    val title: String = "正在解析",
    val message: String = ""
)