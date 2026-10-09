package com.secondbrain.android.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import com.secondbrain.android.data.remote.*
import com.secondbrain.android.ui.theme.SecondBrainTheme
import org.junit.After
import org.junit.Before
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy
import java.security.MessageDigest

class VocabularyTranslationTest {
    @get:Rule val compose = createComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val owners = ViewModelStore()
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
            super.getSharedPreferences("vocabulary_translation_tests_$name", mode)
    }
    private val original = VocabularyArticle(12, listOf("habit", "quiet"), "A habit makes learning easier.",
        missingWords = listOf("quiet"), topic = "学习的日常", difficulty = "中级")

    @Before fun prepare() {
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After fun cleanup() {
        instrumentation.runOnMainSync { owners.clear() }
        context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test fun readsFullTranslationAndReopensWithoutAnotherModelRequest() {
        var calls = 0
        val reading = original.copy(article = original.article + "\n\nSmall steps help.")
        show(api { name -> when (name) {
            "translateVocabularyArticle" -> { calls++; ApiResult(200, "ok", translated(reading, "习惯让学习更轻松。\n\n这是完整的第二段译文。")) }
            else -> common(name, reading)
        } })
        compose.onNodeWithText("查看完整翻译").performClick()
        compose.onNodeWithText("完整翻译").assertIsDisplayed()
        compose.onNodeWithText("习惯让学习更轻松。").assertIsDisplayed()
        compose.onNodeWithText("这是完整的第二段译文。").assertIsDisplayed()
        capture()
        compose.onNodeWithText("返回英文").performClick()
        compose.onNodeWithText(original.article).assertIsDisplayed()
        compose.onNodeWithText("查看完整翻译").performClick()
        compose.onNodeWithText("习惯让学习更轻松。").assertIsDisplayed()
        assertEquals(1, calls)
    }

    @Test fun translationFailureCanRetryAndOriginalRemainsReadable() {
        var calls = 0
        show(api { name -> when (name) {
            "translateVocabularyArticle" -> {
                if (++calls == 1) error("翻译服务暂不可用")
                ApiResult(200, "ok", translated(original, "重试后的完整译文。"))
            }
            else -> common(name, original)
        } })
        compose.onNodeWithText("查看完整翻译").performClick()
        compose.onNodeWithText("重试全文翻译").performClick()
        compose.onNodeWithText("重试后的完整译文。").assertIsDisplayed()
        compose.onNodeWithText("返回英文").performClick()
        compose.onNodeWithText(original.article).assertIsDisplayed()
        assertEquals(2, calls)
    }

    @Test fun supplementaryReadingInvalidatesPreviousTranslation() {
        var current = original
        var calls = 0
        show(api { name -> when (name) {
            "translateVocabularyArticle" -> {
                calls++
                ApiResult(200, "ok", translated(current,
                    if (current.missingWords.isEmpty()) "习惯让学习更轻松。\n\n补充阅读\n\n房间很安静。" else "习惯让学习更轻松。"))
            }
            "completeVocabularyArticle" -> {
                current = original.copy(article = original.article + "\n\n补充阅读\n\nThe room is quiet.", missingWords = emptyList())
                ApiResult(200, "ok", current)
            }
            else -> common(name, current)
        } })
        compose.onNodeWithText("查看完整翻译").performClick()
        compose.onNodeWithText("返回英文").performClick()
        compose.onNodeWithText("生成补充阅读").performScrollTo().performClick()
        compose.onNodeWithText("查看完整翻译").performScrollTo().performClick()
        compose.onNodeWithText("房间很安静。").assertIsDisplayed()
        assertEquals(2, calls)
    }

    @Test fun mismatchedBodyVersionDoesNotDisplayStaleTranslation() {
        show(api { name -> when (name) {
            "translateVocabularyArticle" -> ApiResult(200, "ok", VocabularyArticleTranslation(12, "old-hash", "过期译文"))
            else -> common(name, original)
        } })
        compose.onNodeWithText("查看完整翻译").performClick()
        compose.onNodeWithText("重试全文翻译").assertIsDisplayed()
        compose.onNodeWithText("过期译文").assertDoesNotExist()
        compose.onNodeWithText("返回英文").performClick()
        compose.onNodeWithText(original.article).assertIsDisplayed()
    }

    private fun show(api: SecondBrainApi) {
        lateinit var vm: VocabularyArticleViewModel
        instrumentation.runOnMainSync {
            vm = VocabularyArticleViewModel(api, context).also { owners.put("reader", it) }
            vm.openArticle(12)
        }
        compose.setContent { SecondBrainTheme { Surface { VocabularyArticleScreen({}, vm) } } }
    }

    private fun common(name: String, article: VocabularyArticle): Any = when (name) {
        "vocabularyArticles" -> ApiResult(200, "ok", emptyList<VocabularyArticleSummary>())
        "vocabularyVisionReady" -> ApiResult(200, "ok", VisionReadiness(true, "ready"))
        "vocabularyArticle" -> ApiResult(200, "ok", article)
        else -> error(name)
    }

    private fun translated(article: VocabularyArticle, text: String): VocabularyArticleTranslation {
        val hash = MessageDigest.getInstance("SHA-256").digest(article.article.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return VocabularyArticleTranslation(article.id, hash, text)
    }

    private fun api(reply: (String) -> Any): SecondBrainApi = Proxy.newProxyInstance(
        SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)
    ) { _, method, _ -> reply(method.name) } as SecondBrainApi

    private fun capture() {
        compose.waitForIdle()
        // 系统截图包含独立的弹层窗口，避免只截到其下方的英文阅读页。
        instrumentation.uiAutomation.waitForIdle(500, 3_000)
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        File(context.getExternalFilesDir(null), "vocabulary-translation.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
