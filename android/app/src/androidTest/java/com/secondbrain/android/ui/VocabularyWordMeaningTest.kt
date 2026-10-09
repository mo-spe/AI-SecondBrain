package com.secondbrain.android.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
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
        return vm
    }

    private fun answer(article: VocabularyArticle, request: VocabularyWordMeaningRequest,
                       meaning: String): VocabularyWordMeaning {
        val selection = checkNotNull(readingWordAt(article.article, request.start, request.end))
        return VocabularyWordMeaning(article.id, request.sourceHash, request.start, request.end,
            selection.word, meaning, selection.sentence)
    }
}
