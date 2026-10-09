package com.secondbrain.android.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.secondbrain.android.data.remote.*
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resumeWithException

class VocabularyRecoveryTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val owners = ViewModelStore()
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("vocabulary_recovery_tests_$name", mode)
    }

    @Before fun prepare() {
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        instrumentation.runOnMainSync { owners.clear() }
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun lostResponsePreservesDraftRefreshesSavedHistoryAndRejectsDuplicateTap() {
        var writes = 0
        var historyReads = 0
        var pending: Continuation<Any?>? = null
        val saved = VocabularyArticleSummary(12, "日常与学习", "中级", 1, 1)
        val api = api { name, args ->
            when (name) {
                "vocabularyArticles" -> ApiResult(200, "ok", if (++historyReads > 1) listOf(saved) else emptyList())
                "vocabularyVisionReady" -> ApiResult(200, "ok", VisionReadiness(true, "ready"))
                "generateVocabularyArticle" -> {
                    writes++
                    @Suppress("UNCHECKED_CAST")
                    pending = args.last() as Continuation<Any?>
                    COROUTINE_SUSPENDED
                }
                else -> error(name)
            }
        }
        lateinit var vm: VocabularyArticleViewModel
        instrumentation.runOnMainSync {
            vm = VocabularyArticleViewModel(api, context).also { owners.put("generation", it) }
            vm.addWord(); vm.editWord(0, "habit"); vm.confirm()
            vm.generate(); vm.generate()
            assertTrue(vm.state.value.busy)
        }
        instrumentation.waitForIdleSync()
        assertEquals(1, writes)
        instrumentation.runOnMainSync { pending!!.resumeWithException(IOException("reset")) }
        instrumentation.waitForIdleSync()
        assertFalse(vm.state.value.busy)
        assertTrue(vm.state.value.confirmed)
        assertEquals("habit", vm.state.value.candidates.single().word)
        assertEquals(12L, vm.state.value.history.single().id)
        assertTrue(vm.state.value.message!!.contains("可能已保存"))
        instrumentation.runOnMainSync {
            val restored = VocabularyArticleViewModel(api, context).also { owners.put("restored", it) }
            assertTrue(restored.state.value.confirmed)
            assertEquals("habit", restored.state.value.candidates.single().word)
        }
    }

    @Test fun failedCompletionKeepsReadingAndCanReopenSavedUpdate() {
        val original = VocabularyArticle(12, listOf("habit"), "Original reading.", missingWords = listOf("habit"),
            topic = "日常与学习", difficulty = "中级")
        val updated = original.copy(article = "Original reading. A habit helps.", missingWords = emptyList())
        var reads = 0
        var writes = 0
        var pending: Continuation<Any?>? = null
        lateinit var vm: VocabularyArticleViewModel
        instrumentation.runOnMainSync {
            vm = VocabularyArticleViewModel(api { name, args -> when (name) {
                "vocabularyArticles" -> ApiResult(200, "ok", emptyList<VocabularyArticleSummary>())
                "vocabularyVisionReady" -> ApiResult(200, "ok", VisionReadiness(true, "ready"))
                "vocabularyArticle" -> ApiResult(200, "ok", if (++reads == 1) original else updated)
                "completeVocabularyArticle" -> {
                    writes++
                    @Suppress("UNCHECKED_CAST")
                    pending = args.last() as Continuation<Any?>
                    COROUTINE_SUSPENDED
                }
                else -> error(name)
            } }, context).also { owners.put("completion", it) }
            vm.openArticle(12)
        }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync { vm.completeMissingWords(); vm.completeMissingWords() }
        instrumentation.waitForIdleSync()
        assertEquals(1, writes)
        instrumentation.runOnMainSync { pending!!.resumeWithException(IOException("reset")) }
        instrumentation.waitForIdleSync()
        assertEquals(original, vm.state.value.article)
        assertFalse(vm.state.value.busy)
        assertTrue(vm.state.value.message!!.contains("可能已保存"))
        instrumentation.runOnMainSync { vm.closeArticle(); vm.openArticle(12) }
        instrumentation.waitForIdleSync()
        assertEquals(updated, vm.state.value.article)
    }

    private fun api(reply: (String, Array<out Any?>) -> Any?): SecondBrainApi = Proxy.newProxyInstance(
        SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)
    ) { _, method, args -> reply(method.name, args ?: emptyArray()) } as SecondBrainApi
}
