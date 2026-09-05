package com.secondbrain.android.review

import org.junit.Assert.*
import org.junit.Test

class ReviewPromptTest {
    @Test fun separatesQuestionOptionsAndExplanation() {
        val prompt = parseReviewPrompt("构件化开发的优点是什么？\nA. 降低可靠性\nB. 提高可靠性\n正确答案：B\n解析：经多场景验证更稳定。\n选项解析：\nA. 错误\nB. 正确")
        assertEquals("构件化开发的优点是什么？", prompt.question)
        assertEquals(listOf("A", "B"), prompt.options.map { it.key })
        assertEquals("提高可靠性", prompt.options.last().text)
        assertTrue(prompt.explanation.contains("经多场景验证"))
        assertFalse(prompt.question.contains("正确答案"))
    }

    @Test fun explanationOnlyCardNeverLeaksAsQuestion() {
        val prompt = parseReviewPrompt("正确答案：C\n解析：这是一段不能提前看到的解析。")
        assertEquals("", prompt.question)
        assertTrue(prompt.options.isEmpty())
    }

    @Test fun supportsChinesePunctuationAndMultilineOptions() {
        val prompt = parseReviewPrompt("请选择\r\nA、第一项\r\n补充说明\r\nB）第二项\r\n【解析】解释")
        assertEquals("请选择", prompt.question)
        assertEquals("第一项\n补充说明", prompt.options.first().text)
        assertEquals("第二项", prompt.options.last().text)
    }

    @Test fun preservesOpenQuestionWithoutAnswerMarkers() {
        val prompt = parseReviewPrompt("说明 Redis 持久化的差别。")
        assertEquals("说明 Redis 持久化的差别。", prompt.question)
        assertTrue(prompt.options.isEmpty())
    }

    @Test fun ignoresOptionsInsideExplanation() {
        val prompt = parseReviewPrompt("简述设计原因。\n解析：\nA. 原因之一\nB. 原因之二")
        assertTrue(prompt.options.isEmpty())
        assertEquals("简述设计原因。", prompt.question)
    }
    @Test fun preservesAnswerWordsInsideQuestionAndHidesBoldAnswerLabels() {
        val prompt = parseReviewPrompt("请选择正确答案。\nA. 选项一\nB. 选项二\n**正确答案**：B\n解析：说明")
        assertEquals("请选择正确答案。", prompt.question)
        assertEquals(2, prompt.options.size)
        assertFalse(prompt.options.last().text.contains("正确答案"))
    }
}
