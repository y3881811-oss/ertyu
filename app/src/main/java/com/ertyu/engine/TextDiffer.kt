package com.ertyu.engine

import com.ertyu.model.DiffLine
import com.ertyu.model.DiffLineType

/**
 * 基于 LCS（最长公共子序列）的文本差分。
 * 返回行级差异列表。
 */
object TextDiffer {

    fun diff(leftText: String, rightText: String): List<DiffLine> {
        val leftLines = leftText.split('\n')
        val rightLines = rightText.split('\n')

        val n = leftLines.size
        val m = rightLines.size

        // LCS 表：lcs[i][j] 表示 leftLines[i..] 和 rightLines[j..] 的 LCS 长度
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcs[i][j] = if (leftLines[i] == rightLines[j]) {
                    lcs[i + 1][j + 1] + 1
                } else {
                    maxOf(lcs[i + 1][j], lcs[i][j + 1])
                }
            }
        }

        // 回溯
        val result = ArrayList<DiffLine>()
        var i = 0
        var j = 0
        var leftLineNo = 1
        var rightLineNo = 1

        while (i < n && j < m) {
            if (leftLines[i] == rightLines[j]) {
                result.add(
                    DiffLine(
                        leftLineNo = leftLineNo,
                        rightLineNo = rightLineNo,
                        leftText = leftLines[i],
                        rightText = rightLines[j],
                        type = DiffLineType.SAME
                    )
                )
                i++
                j++
                leftLineNo++
                rightLineNo++
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                // 删除了左行
                result.add(
                    DiffLine(
                        leftLineNo = leftLineNo,
                        rightLineNo = null,
                        leftText = leftLines[i],
                        rightText = "",
                        type = DiffLineType.REMOVED
                    )
                )
                i++
                leftLineNo++
            } else {
                // 新增了右行
                result.add(
                    DiffLine(
                        leftLineNo = null,
                        rightLineNo = rightLineNo,
                        leftText = "",
                        rightText = rightLines[j],
                        type = DiffLineType.ADDED
                    )
                )
                j++
                rightLineNo++
            }
        }
        while (i < n) {
            result.add(
                DiffLine(
                    leftLineNo = leftLineNo,
                    rightLineNo = null,
                    leftText = leftLines[i],
                    rightText = "",
                    type = DiffLineType.REMOVED
                )
            )
            i++
            leftLineNo++
        }
        while (j < m) {
            result.add(
                DiffLine(
                    leftLineNo = null,
                    rightLineNo = rightLineNo,
                    leftText = "",
                    rightText = rightLines[j],
                    type = DiffLineType.ADDED
                )
            )
            j++
            rightLineNo++
        }

        // 合并相邻的 REMOVED + ADDED 为 MODIFIED
        return mergeModified(result)
    }

    private fun mergeModified(lines: List<DiffLine>): List<DiffLine> {
        val out = ArrayList<DiffLine>(lines.size)
        var i = 0
        while (i < lines.size) {
            val cur = lines[i]
            if (cur.type == DiffLineType.REMOVED &&
                i + 1 < lines.size &&
                lines[i + 1].type == DiffLineType.ADDED
            ) {
                val next = lines[i + 1]
                out.add(
                    DiffLine(
                        leftLineNo = cur.leftLineNo,
                        rightLineNo = next.rightLineNo,
                        leftText = cur.leftText,
                        rightText = next.rightText,
                        type = DiffLineType.MODIFIED
                    )
                )
                i += 2
            } else {
                out.add(cur)
                i++
            }
        }
        return out
    }
}