package com.ertyu

/**
 * 记录左右两个面板的滚动位置。
 * - 从详情页（编辑器/图片/APK 等）返回时，滚动位置保持不变
 * - 切换目录时，位置自动重置为顶部
 */
object FilePanelScrollStore {
    // 左面板
    var leftIndex: Int = 0
    var leftOffset: Int = 0
    var leftUri: String = ""

    // 右面板
    var rightIndex: Int = 0
    var rightOffset: Int = 0
    var rightUri: String = ""

    fun savedIndex(isLeft: Boolean): Int =
        if (isLeft) leftIndex else rightIndex

    fun savedOffset(isLeft: Boolean): Int =
        if (isLeft) leftOffset else rightOffset

    fun savedUri(isLeft: Boolean): String =
        if (isLeft) leftUri else rightUri

    fun save(isLeft: Boolean, uri: String, index: Int, offset: Int) {
        if (isLeft) {
            leftUri = uri
            leftIndex = index
            leftOffset = offset
        } else {
            rightUri = uri
            rightIndex = index
            rightOffset = offset
        }
    }
}