package com.novaproject.novai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novaproject.novai.ui.theme.*

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier
) {
    val blocks = parseMarkdownBlocks(text)

    SelectionContainer {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (block in blocks) {
                when (block) {
                    is MdBlock.CodeBlock -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NovDark)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = block.code,
                                color = NovCyan,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    is MdBlock.Paragraph -> {
                        Text(
                            text = parseInlineMarkdown(block.text),
                            color = NovTextPrimary,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        )
                    }
                    is MdBlock.Header -> {
                        val (size, weight) = when (block.level) {
                            1 -> Pair(20.sp, FontWeight.ExtraBold)
                            2 -> Pair(18.sp, FontWeight.Bold)
                            else -> Pair(16.sp, FontWeight.SemiBold)
                        }
                        Text(
                            text = parseInlineMarkdown(block.text),
                            color = NovTextPrimary,
                            fontSize = size,
                            fontWeight = weight,
                            lineHeight = (size.value * 1.4f).sp
                        )
                    }
                    is MdBlock.ListItem -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = block.bullet,
                                color = NovCyan,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(24.dp)
                            )
                            Text(
                                text = parseInlineMarkdown(block.text),
                                color = NovTextPrimary,
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    is MdBlock.Divider -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(NovDivider)
                        )
                    }
                }
            }
        }
    }
}

sealed class MdBlock {
    data class Paragraph(val text: String) : MdBlock()
    data class Header(val level: Int, val text: String) : MdBlock()
    data class ListItem(val bullet: String, val text: String) : MdBlock()
    data class CodeBlock(val code: String, val language: String = "") : MdBlock()
    object Divider : MdBlock()
}

fun parseMarkdownBlocks(input: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = input.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MdBlock.CodeBlock(codeLines.joinToString("\n"), lang))
            i++
            continue
        }

        // Header
        val headerMatch = Regex("^(#{1,6})\\s+(.+)$").find(line)
        if (headerMatch != null) {
            val level = headerMatch.groupValues[1].length
            blocks.add(MdBlock.Header(level, headerMatch.groupValues[2]))
            i++
            continue
        }

        // Horizontal rule
        if (line.matches(Regex("^[-*_]{3,}\\s*$"))) {
            blocks.add(MdBlock.Divider)
            i++
            continue
        }

        // Unordered list
        val ulMatch = Regex("^\\s*[-*+]\\s+(.+)$").find(line)
        if (ulMatch != null) {
            blocks.add(MdBlock.ListItem("•", ulMatch.groupValues[1]))
            i++
            continue
        }

        // Ordered list
        val olMatch = Regex("^\\s*(\\d+)\\.\\s+(.+)$").find(line)
        if (olMatch != null) {
            blocks.add(MdBlock.ListItem("${olMatch.groupValues[1]}.", olMatch.groupValues[2]))
            i++
            continue
        }

        // Empty line — skip
        if (line.isBlank()) {
            i++
            continue
        }

        // Paragraph — collect until blank or special
        val paragraphLines = mutableListOf<String>()
        while (i < lines.size) {
            val l = lines[i]
            if (l.isBlank()) break
            if (l.trimStart().startsWith("```")) break
            if (Regex("^#{1,6}\\s").containsMatchIn(l)) break
            if (Regex("^\\s*[-*+]\\s").containsMatchIn(l)) break
            if (Regex("^\\s*\\d+\\.\\s").containsMatchIn(l)) break
            if (l.matches(Regex("^[-*_]{3,}\\s*$"))) break
            paragraphLines.add(l)
            i++
        }
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MdBlock.Paragraph(paragraphLines.joinToString(" ")))
        }
    }

    return blocks
}

fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        // Bold+italic ***
        if (text.startsWith("***", i) || text.startsWith("___", i)) {
            val delim = text.substring(i, i + 3)
            val end = text.indexOf(delim, i + 3)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                    append(parseInlineMarkdown(text.substring(i + 3, end)))
                }
                i = end + 3
                continue
            }
        }
        // Bold **
        if (text.startsWith("**", i) || text.startsWith("__", i)) {
            val delim = text.substring(i, i + 2)
            val end = text.indexOf(delim, i + 2)
            if (end != -1) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(text.substring(i + 2, end))
                }
                i = end + 2
                continue
            }
        }
        // Italic * or _
        if ((text[i] == '*' || text[i] == '_') && (i == 0 || text[i - 1] == ' ')) {
            val delim = text[i]
            val end = text.indexOf(delim, i + 1)
            if (end != -1 && end > i + 1) {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        // Inline code `
        if (text[i] == '`') {
            val end = text.indexOf('`', i + 1)
            if (end != -1) {
                withStyle(SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = NovDark.copy(alpha = 0.8f),
                    color = NovCyan,
                    fontSize = 13.sp
                )) {
                    append(text.substring(i + 1, end))
                }
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
