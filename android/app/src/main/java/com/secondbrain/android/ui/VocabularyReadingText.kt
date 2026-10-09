package com.secondbrain.android.ui

import java.security.MessageDigest
import java.text.BreakIterator
import java.util.Locale

internal val readingWordPattern = Regex("[A-Za-z]+(?:['’\\-][A-Za-z]+)*")

internal data class ReadingParagraph(val text: String, val start: Int)

data class ReadingWordSelection(val word: String, val start: Int, val end: Int, val sentence: String)

internal fun readingParagraphs(source: String): List<ReadingParagraph> {
    var cursor = 0
    return source.split(Regex("\\R\\s*\\R")).map(String::trim).filter(String::isNotBlank).map { text ->
        val start = source.indexOf(text, cursor)
        cursor = start + text.length
        ReadingParagraph(text, start)
    }
}

internal fun readingWordAt(source: String, start: Int, end: Int): ReadingWordSelection? {
    if (start < 0 || end <= start || end > source.length || end - start > 100) return null
    val match = readingWordPattern.findAll(source).firstOrNull { it.range.first == start && it.range.last + 1 == end }
        ?: return null
    val lineStart = source.lastIndexOf('\n', (start - 1).coerceAtLeast(0)) + 1
    val lineEnd = source.indexOf('\n', end).let { if (it < 0) source.length else it }
    val line = source.substring(lineStart, lineEnd)
    val sentences = BreakIterator.getSentenceInstance(Locale.ENGLISH).apply { setText(line) }
    val sentenceStart = sentences.preceding(start - lineStart + 1).let { if (it < 0) 0 else it }
    val sentenceEnd = sentences.following(start - lineStart).let { if (it < 0) line.length else it }
    val from = maxOf(lineStart + sentenceStart, start - 400)
    val to = minOf(lineStart + sentenceEnd, end + 400)
    return ReadingWordSelection(match.value, start, end, source.substring(from, to).trim())
}

internal fun readingSourceHash(source: String): String = MessageDigest.getInstance("SHA-256")
    .digest(source.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
