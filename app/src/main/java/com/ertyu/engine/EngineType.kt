package com.ertyu.engine

enum class EngineType(val displayName: String, val description: String) {
    NORMAL("普通文件引擎", "无权限要求，最稳定"),
    SHIZUKU("Shizuku 引擎", "免 Root 访问受限目录 (Android 11+)"),
    ROOT("Root 引擎", "完全控制文件系统")
}