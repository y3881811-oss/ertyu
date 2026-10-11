package com.ertyu.model

enum class SortMode(val label: String) {
    NAME_ASC("名称 ↑"),
    NAME_DESC("名称 ↓"),
    SIZE_ASC("大小 ↑"),
    SIZE_DESC("大小 ↓"),
    TIME_ASC("时间 ↑"),
    TIME_DESC("时间 ↓"),
    TYPE_ASC("类型 ↑"),
    TYPE_DESC("类型 ↓")
}