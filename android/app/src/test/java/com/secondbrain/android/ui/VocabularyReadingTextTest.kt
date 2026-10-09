package com.secondbrain.android.ui

import org.junit.Assert.*
import org.junit.Test

class VocabularyReadingTextTest {
    @Test fun duplicateParagraphsKeepTheirOwnOriginalPositions() {
        val source = "  A bank.\r\n\r\n补充阅读\r\n\r\n  A bank.  "
        val sections = readingParagraphs(source)
        assertEquals(listOf("A bank.", "补充阅读", "A bank."), sections.map { it.text })
        assertEquals(source.indexOf("A bank."), sections.first().start)
        assertEquals(source.lastIndexOf("A bank."), sections.last().start)
        sections.forEach { assertEquals(it.text, source.substring(it.start, it.start + it.text.length)) }
    }

    @Test fun secondOccurrenceUsesSecondSentence() {
        val source = "A bank lends money. We sat on the bank."
        val second = source.lastIndexOf("bank")
        assertEquals("We sat on the bank.", readingWordAt(source, second, second + 4)?.sentence)
        assertEquals("A bank lends money.", readingWordAt(source, 2, 6)?.sentence)
    }

    @Test fun punctuationIsExcludedButInternalApostrophesAndHyphensRemain() {
        val source = "🌿 'Don’t' forget well-being; she's here."
        val matches = readingWordPattern.findAll(source).toList()
        assertEquals(listOf("Don’t", "forget", "well-being", "she's", "here"), matches.map { it.value })
        matches.forEach { assertEquals(it.value, readingWordAt(source, it.range.first, it.range.last + 1)?.word) }
        assertNull(readingWordAt(source, matches.first().range.first + 1, matches.first().range.last + 1))
        assertNull(readingWordAt(source, -1, 2))
    }

    @Test fun headingAndOtherParagraphsDoNotEnterWordContext() {
        val source = "Previous text.\n\n补充阅读\n\nA quiet room."
        val start = source.indexOf("quiet")
        assertEquals("A quiet room.", readingWordAt(source, start, start + 5)?.sentence)
    }

    @Test fun contextHasBoundedLengthWithoutSentencePunctuation() {
        val source = "before ".repeat(200) + "bank " + "after ".repeat(200)
        val start = source.indexOf("bank")
        val context = readingWordAt(source, start, start + 4)!!.sentence
        assertTrue(context.contains("bank"))
        assertTrue(context.length <= 804)
        assertNotEquals(readingSourceHash(source), readingSourceHash(source + "changed"))
    }
}
