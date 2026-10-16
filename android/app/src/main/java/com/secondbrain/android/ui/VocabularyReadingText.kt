package com.secondbrain.android.ui

import androidx.compose.runtime.Immutable
import java.security.MessageDigest
import java.text.BreakIterator
import java.util.Locale

internal val readingWordPattern = Regex("[A-Za-z]+(?:['’\\-][A-Za-z]+)*")

@Immutable
internal data class ReadingToken(val word: String, val start: Int, val end: Int)

@Immutable
internal data class ReadingParagraph(val text: String, val start: Int, val words: List<ReadingToken>)

data class ReadingWordSelection(val word: String, val start: Int, val end: Int, val sentence: String)

internal fun readingParagraphs(source: String): List<ReadingParagraph> {
    var cursor = 0
    return source.split(Regex("\\R\\s*\\R")).map(String::trim).filter(String::isNotBlank).map { text ->
        val start = source.indexOf(text, cursor)
        cursor = start + text.length
        ReadingParagraph(text, start, readingWordPattern.findAll(text).map {
            ReadingToken(it.value, start + it.range.first, start + it.range.last + 1)
        }.toList())
    }
}

internal fun readingWordAt(source: String, start: Int, end: Int): ReadingWordSelection? {
    if (start < 0 || end <= start || end > source.length || end - start > 100) return null
    return VocabularyReadingDocument(source).wordAt(start, end)
}

/** 在后台为正文建立一次索引，点词时无需在界面线程重新扫描长文或计算指纹。 */
@Immutable
class VocabularyReadingDocument(val source: String) {
    val sourceHash: String = readingSourceHash(source)
    internal val paragraphs = readingParagraphs(source)
    private val words = paragraphs.flatMap { it.words }.associateBy { it.start }
    private val sentences = buildList {
        val iterator = BreakIterator.getSentenceInstance(Locale.ENGLISH)
        Regex("[^\\r\\n]+").findAll(source).forEach { line ->
            iterator.setText(line.value)
            var from = iterator.first()
            var to = iterator.next()
            while (to != BreakIterator.DONE) {
                add((line.range.first + from) until (line.range.first + to))
                from = to
                to = iterator.next()
            }
        }
    }

    fun wordAt(start: Int, end: Int): ReadingWordSelection? {
        val token = words[start]?.takeIf { it.end == end && end - start <= 100 } ?: return null
        val index = sentences.binarySearch { range -> when {
            range.last < start -> -1
            range.first > start -> 1
            else -> 0
        } }
        if (index < 0) return null
        val sentence = sentences[index]
        return ReadingWordSelection(token.word, start, end,
            source.substring(maxOf(sentence.first, start - 400), minOf(sentence.last + 1, end + 400)).trim())
    }
}

internal fun ReadingParagraph.wordAtOffset(offset: Int): ReadingToken? {
    val index = words.binarySearch { token -> when {
        token.end <= offset -> -1
        token.start > offset -> 1
        else -> 0
    } }
    return words.getOrNull(index)
}

internal fun readingSourceHash(source: String): String = MessageDigest.getInstance("SHA-256")
    .digest(source.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
