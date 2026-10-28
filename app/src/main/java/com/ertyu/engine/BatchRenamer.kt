package com.ertyu.engine

import com.ertyu.model.NumberPosition

object BatchRenamer {

    fun computeName(
        original: String,
        index: Int,
        prefix: String,
        suffix: String,
        findText: String,
        replaceText: String,
        useRegex: Boolean,
        useNumbering: Boolean,
        numberStart: Int,
        numberStep: Int,
        numberDigits: Int,
        numberPosition: NumberPosition
    ): String {
        val dotIdx = original.lastIndexOf('.')
        val (base, ext) = if (dotIdx > 0) {
            original.substring(0, dotIdx) to original.substring(dotIdx)
        } else {
            original to ""
        }

        var newBase = base

        if (findText.isNotEmpty()) {
            newBase = if (useRegex) {
                runCatching { newBase.replace(Regex(findText), replaceText) }
                    .getOrDefault(newBase)
            } else {
                newBase.replace(findText, replaceText)
            }
        }

        newBase = "$prefix$newBase$suffix"

        if (useNumbering) {
            val num = numberStart + index * numberStep
            val numStr = num.toString().padStart(numberDigits, '0')
            newBase = when (numberPosition) {
                NumberPosition.PREFIX -> "$numStr $newBase"
                NumberPosition.SUFFIX -> "$newBase $numStr"
            }
        }

        return newBase + ext
    }
}