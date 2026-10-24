package com.ertyu

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

object EditorHighlighter {
    private val COLOR_KEYWORD = Color(0xFF0000FF)
    private val COLOR_STRING = Color(0xFF008000)
    private val COLOR_COMMENT = Color(0xFF808080)

    fun highlight(text: String, ext: String): AnnotatedString {
        return buildAnnotatedString {
            append(text)
            if (ext in listOf("kt", "java", "kts", "xml", "json", "gradle", "properties", "yml", "yaml")) {
                val commentRegex = Regex("""(//.*|/\*[\s\S]*?\*/)""")
                commentRegex.findAll(text).forEach {
                    addStyle(SpanStyle(color = COLOR_COMMENT), it.range.first, it.range.last + 1)
                }
                val stringRegex = Regex("""("(?:\\.|[^"\\])*")""")
                stringRegex.findAll(text).forEach {
                    addStyle(SpanStyle(color = COLOR_STRING), it.range.first, it.range.last + 1)
                }
                if (ext in listOf("kt", "java", "kts", "gradle")) {
                    val keywordRegex = Regex("""\b(fun|val|var|class|object|interface|if|else|when|for|while|return|import|package|public|private|protected|override|open|abstract|suspend|data|sealed|companion|true|false|null|this|super|is|as|in|out|throw|try|catch|finally|new)\b""")
                    keywordRegex.findAll(text).forEach {
                        addStyle(SpanStyle(color = COLOR_KEYWORD), it.range.first, it.range.last + 1)
                    }
                }
            }
        }
    }
}

class SyntaxHighlightTransformation(private val ext: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val highlighted = EditorHighlighter.highlight(text.text, ext)
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}