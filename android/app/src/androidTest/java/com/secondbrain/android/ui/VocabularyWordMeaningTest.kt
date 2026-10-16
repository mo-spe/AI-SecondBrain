package com.secondbrain.android.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.os.SystemClock
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.secondbrain.android.data.remote.*
import com.secondbrain.android.ui.theme.SecondBrainTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.lang.reflect.Proxy
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume

class VocabularyWordMeaningTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val owners = ViewModelStore()
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("vocabulary_word_tests_$name", mode)
    }
    private val original = VocabularyArticle(12, listOf("habit"),
        "A habit makes learning easier.", meanings = mapOf("habit" to "习惯"),
        topic = "学习的日常", difficulty = "中级")

    @Before fun prepare() {
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        instrumentation.runOnMainSync { owners.clear() }
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun existingTargetMeaningStillOpensWithoutNetwork() {
        var calls = 0
        show(original) { _, _ -> calls++; error("目标词不应调用网络") }
        tapWord("habit")
        compose.onNodeWithText("习惯").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithText(original.article).assertIsDisplayed()
        assertEquals(0, calls)
    }

    @Test fun ordinaryWordOpensAndReusesMeaningOnSecondTap() {
        var calls = 0
        show(original) { request, _ ->
            calls++
            ApiResult(200, "ok", answer(original, request, "学习"))
        }
        tapWord("learning")
        compose.onNodeWithText("学习").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        tapWord("learning")
        compose.onNodeWithText("学习").assertIsDisplayed()
        assertEquals(1, calls)
    }

    @Test fun secondOccurrenceUsesActualClickedSentenceAndSeparateCache() {
        val article = original.copy(article = "A bank lends money. We sat on the bank.")
        val positions = mutableListOf<Int>()
        show(article) { request, _ ->
            positions.add(request.start)
            ApiResult(200, "ok", answer(article, request, if (request.start == 2) "银行" else "河岸"))
        }
        tapWord("bank", occurrence = 1)
        compose.onNodeWithText("河岸").assertIsDisplayed()
        compose.onNodeWithText("We sat on the bank.").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        tapWord("bank")
        compose.onNodeWithText("银行").assertIsDisplayed()
        assertEquals(listOf(article.article.lastIndexOf("bank"), 2), positions)
    }

    @Test fun failedLookupCanRetryWithoutLosingReading() {
        var calls = 0
        show(original) { request, _ ->
            if (++calls == 1) error("释义服务暂不可用")
            ApiResult(200, "ok", answer(original, request, "学习"))
        }
        tapWord("learning")
        compose.onNodeWithText("重试释义").performClick()
        compose.onNodeWithText("学习").assertIsDisplayed()
        compose.onNodeWithText("关闭").performClick()
        compose.onNodeWithText(original.article).assertIsDisplayed()
        assertEquals(2, calls)
    }

    @Test fun staleBodyResponseIsNotShown() {
        show(original) { request, _ ->
            ApiResult(200, "ok", answer(original, request, "过期释义").copy(sourceHash = "old-hash"))
        }
        tapWord("learning")
        compose.onNodeWithText("重试释义").assertIsDisplayed()
        compose.onNodeWithText("过期释义").assertDoesNotExist()
    }

    @Test fun duplicateTapAndLateResponseCannotReplaceNewSelection() {
        var calls = 0
        lateinit var pending: Continuation<Any?>
        lateinit var request: VocabularyWordMeaningRequest
        val vm = show(original) { value, continuation ->
            calls++
            request = value
            pending = continuation
            COROUTINE_SUSPENDED
        }
        val learning = original.article.indexOf("learning")
        instrumentation.runOnMainSync {
            vm.selectWord(learning, learning + 8)
            vm.selectWord(learning, learning + 8)
        }
        compose.onNodeWithText("正在查询这句话中的释义…").assertIsDisplayed()
        instrumentation.runOnMainSync {
            vm.selectWord(2, 7)
            pending.resume(ApiResult(200, "ok", answer(original, request, "迟到的释义")))
        }
        compose.onNodeWithText("习惯").assertIsDisplayed()
        compose.onNodeWithText("迟到的释义").assertDoesNotExist()
        assertEquals(1, calls)
        instrumentation.runOnMainSync { vm.dismissWord() }
        assertNull(vm.state.value.wordLookup)
    }

    @Test fun openingHistoryShowsReadProgressWithoutWritingOrModelCall() {
        lateinit var pending: Continuation<Any?>
        var writes = 0
        val api = Proxy.newProxyInstance(SecondBrainApi::class.java.classLoader,
            arrayOf(SecondBrainApi::class.java)) { _, method, args -> when (method.name) {
            "vocabularyArticles" -> ApiResult(200, "ok",
                listOf(VocabularyArticleSummary(12, "旧文示例", "中级", 1, 1)))
            "vocabularyVisionReady" -> ApiResult(200, "ok", VisionReadiness(true, "ready"))
            "vocabularyArticle" -> {
                @Suppress("UNCHECKED_CAST")
                val continuation = args.last() as Continuation<Any?>
                pending = continuation
                COROUTINE_SUSPENDED
            }
            "generateVocabularyArticle" -> { writes++; error("读取不能触发写作") }
            else -> error(method.name)
        } } as SecondBrainApi
        lateinit var vm: VocabularyArticleViewModel
        instrumentation.runOnMainSync {
            vm = VocabularyArticleViewModel(api, context).also { owners.put("history", it) }
            vm.addWord(); vm.editWord(0, "habit"); vm.confirm()
        }
        compose.setContent { SecondBrainTheme { Surface { VocabularyArticleScreen({}, vm) } } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("旧文示例"))
        compose.onNodeWithText("旧文示例").performClick()
        compose.onNodeWithText("正在打开文章…").assertIsDisplayed()
        compose.onNodeWithText("正在写作和核验…").assertDoesNotExist()
        assertFalse(vm.state.value.busy)
        assertFalse(vm.state.value.generating)
        assertEquals(12L, vm.state.value.articleLoadingId)
        assertEquals(0, writes)
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("生成一篇文章"))
        compose.onNodeWithText("生成一篇文章").assertIsDisplayed().assertIsNotEnabled()
        instrumentation.runOnMainSync { pending.resume(ApiResult(200, "ok", original)) }
        compose.waitUntil(5_000) { vm.state.value.article != null }
        compose.onNodeWithText(original.article).assertIsDisplayed()
    }

    @Test fun accessibleParagraphCanChooseAnyWordWithoutPerWordLayouts() {
        show(original) { request, _ -> ApiResult(200, "ok", answer(original, request, "学习")) }
        val paragraph = compose.onNodeWithTag("vocabulary-paragraph-0")
        assertTrue(paragraph.fetchSemanticsNode().children.isEmpty())
        paragraph.performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.onNodeWithText("选择单词").assertIsDisplayed()
        compose.onNodeWithText("4. learning").performClick()
        compose.onNodeWithText("学习").assertIsDisplayed()
    }

    @Test fun longReadingScrollsWithoutAccidentalLookupAndTargetMeaningIsImmediate() {
        val paragraph = List(10) { "A habit makes learning easier and keeps daily progress steady." }.joinToString(" ")
        val article = original.copy(article = List(40) { paragraph }.joinToString("\n\n"))
        var calls = 0
        val vm = show(article) { _, _ -> calls++; error("目标词不应调用网络") }
        val durations = mutableListOf<Long>()
        val first = article.article.indexOf("habit")
        val last = article.article.lastIndexOf("habit")
        instrumentation.runOnMainSync {
            repeat(100) { index ->
                val start = if (index % 2 == 0) first else last
                val before = SystemClock.elapsedRealtimeNanos()
                vm.selectWord(start, start + 5)
                durations.add(SystemClock.elapsedRealtimeNanos() - before)
                assertEquals("习惯", vm.state.value.wordLookup?.meaning)
                assertFalse(vm.state.value.wordLookup!!.busy)
                vm.dismissWord()
            }
        }
        repeat(4) { compose.onNodeWithTag("vocabulary-reader").performTouchInput { swipeUp(durationMillis = 300) } }
        compose.onNodeWithText("当前语境").assertDoesNotExist()
        assertEquals(0, calls)
        val sorted = durations.sorted()
        File(context.getExternalFilesDir(null), "reading-performance.txt").writeText(
            "fixtureWords=4000\nlocalTargetLookups=100\nmodelCalls=$calls\n" +
                "p95LookupMicros=${sorted[94] / 1000}\nmaxLookupMicros=${sorted.last() / 1000}\n" +
                "scope=ViewModel synchronous local lookup, excludes sheet animation and real network\n")
    }

    private fun tapWord(word: String, occurrence: Int = 0) {
        val node = compose.onNodeWithTag("vocabulary-paragraph-0")
        val layouts = mutableListOf<TextLayoutResult>()
        node.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        val match = Regex("(?i)(?<![A-Za-z])${Regex.escape(word)}(?![A-Za-z])")
            .findAll(layout.layoutInput.text.text).toList()[occurrence]
        node.performTouchInput { click(layout.getBoundingBox(match.range.first + word.length / 2).center) }
    }

    private fun show(article: VocabularyArticle,
                     reply: (VocabularyWordMeaningRequest, Continuation<Any?>) -> Any): VocabularyArticleViewModel {
        val api = Proxy.newProxyInstance(SecondBrainApi::class.java.classLoader,
            arrayOf(SecondBrainApi::class.java)) { _, method, args ->
            when (method.name) {
                "vocabularyArticles" -> ApiResult(200, "ok", emptyList<VocabularyArticleSummary>())
                "vocabularyVisionReady" -> ApiResult(200, "ok", VisionReadiness(true, "ready"))
                "vocabularyArticle" -> ApiResult(200, "ok", article)
                "vocabularyWordMeaning" -> {
                    @Suppress("UNCHECKED_CAST")
                    reply(args[1] as VocabularyWordMeaningRequest, args.last() as Continuation<Any?>)
                }
                else -> error(method.name)
            }
        } as SecondBrainApi
        lateinit var vm: VocabularyArticleViewModel
        instrumentation.runOnMainSync {
            vm = VocabularyArticleViewModel(api, context).also { owners.put("reader", it) }
            vm.openArticle(article.id)
        }
        compose.setContent { SecondBrainTheme { Surface { VocabularyArticleScreen({}, vm) } } }
        compose.waitUntil(5_000) { vm.state.value.article != null }
        return vm
    }

    private fun answer(article: VocabularyArticle, request: VocabularyWordMeaningRequest,
                       meaning: String): VocabularyWordMeaning {
        val selection = checkNotNull(readingWordAt(article.article, request.start, request.end))
        return VocabularyWordMeaning(article.id, request.sourceHash, request.start, request.end,
            selection.word, meaning, selection.sentence)
    }
}
