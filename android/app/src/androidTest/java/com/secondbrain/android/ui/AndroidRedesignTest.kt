package com.secondbrain.android.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.secondbrain.android.data.remote.ReviewCard
import com.secondbrain.android.ui.theme.SecondBrainTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class AndroidRedesignTest {
    @get:Rule val compose = createComposeRule()

    @Test fun todayActionsRemainAvailableWhenReviewIsEmpty() {
        val actions = mutableListOf<String>()
        compose.setContent {
            SecondBrainTheme(darkTheme = false) {
                Surface {
                    TodayContent(PaddingValues(), LoadState.Empty("今天没有到期的复习卡片"), {},
                        { actions += "rag" }, { actions += "capture" }, { actions += "archive" },
                        { actions += "vocabulary" }, { actions += "review" })
                }
            }
        }
        compose.onNodeWithText("暂无待复习").assertIsNotEnabled()
        listOf("问问知识库", "采集新知识", "错题与疑问", "词表文章").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
        }
        assertEquals(listOf("rag", "capture", "archive", "vocabulary"), actions)
        capture("today-empty")
    }

    @Test fun todayLargeTextKeepsAllEntrypointsReachable() {
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1.6f)) {
                SecondBrainTheme(darkTheme = false) {
                    Surface { TodayContent(PaddingValues(), sampleCards(), {}, {}, {}, {}, {}, {}) }
                }
            }
        }
        compose.onNodeWithText("开始复习").performScrollTo().assertIsEnabled()
        listOf("问问知识库", "采集新知识", "错题与疑问", "词表文章").forEach {
            compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
        }
        capture("today-large-text")
    }

    @Test fun todayRendersReviewAndExploration() {
        var reviewed = 0
        compose.setContent {
            SecondBrainTheme(darkTheme = false) {
                Surface { TodayContent(PaddingValues(), sampleCards(), {}, {}, {}, {}, {}, { reviewed++ }) }
            }
        }
        compose.onNodeWithText("开始复习").performClick()
        assertEquals(1, reviewed)
        capture("today-redesign")
    }

    @Test fun todayDarkPaletteKeepsActionsReadable() {
        compose.setContent {
            SecondBrainTheme(darkTheme = true) {
                Surface { TodayContent(PaddingValues(), sampleCards(), {}, {}, {}, {}, {}, {}) }
            }
        }
        compose.onNodeWithText("开始复习").assertIsEnabled()
        compose.onNodeWithText("词表文章").performScrollTo().assertIsDisplayed()
        capture("today-dark")
    }

    private fun sampleCards() = LoadState.Content(listOf(
        ReviewCard(1, nodeTitle = "构件化开发的优点", question = "构件复用有哪些价值？"),
        ReviewCard(2, nodeTitle = "缓存与数据一致性", question = "如何处理缓存更新？"),
        ReviewCard(3, nodeTitle = "知识如何形成长期记忆", question = "如何组织间隔复习？")
    ))

    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
