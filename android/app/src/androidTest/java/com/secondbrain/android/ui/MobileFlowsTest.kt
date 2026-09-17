package com.secondbrain.android.ui

import android.graphics.Bitmap
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.secondbrain.android.data.session.SessionStore
import com.secondbrain.android.data.repository.KnowledgeRepository
import kotlinx.coroutines.*
import androidx.lifecycle.ViewModelStore
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.secondbrain.android.data.remote.*
import com.secondbrain.android.review.ReviewViewModel
import com.secondbrain.android.ui.theme.SecondBrainTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.lang.reflect.Proxy

class MobileFlowsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun reviewShowsFeedbackOnlyAfterSubmitAndWaitsForNext() {
        var submissions = 0
        val api = fakeApi { name ->
            when (name) {
                "todayReview" -> ApiResult(200, "ok", listOf(
                    ReviewCard(1, nodeTitle = "构件化开发的优点", question = "哪一项是构件化开发的优点？\nA. 降低可靠性\nB. 提高可靠性\n正确答案：B\n解析：经验证的构件更稳定。", cardType = "choice"),
                    ReviewCard(2, nodeTitle = "第二个知识点", question = "请说明复用的价值。")
                ))
                "submitReview" -> { submissions++; ApiResult(200, "ok", ReviewResult(true, "B", "经验证的构件更稳定。")) }
                else -> error(name)
            }
        }
        val vm = ReviewViewModel(api)
        compose.setContent { SecondBrainTheme { Surface { ReviewSessionScreen({}, vm) } } }
        compose.onNodeWithText("提高可靠性").performClick()
        compose.onNodeWithText("答案解析").assertDoesNotExist()
        compose.onNodeWithText("经验证的构件更稳定。").assertDoesNotExist()
        snapshot("review-question")
        compose.onNodeWithText("提交答案").performScrollTo().performClick()
        compose.onNodeWithText("回答正确").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("经验证的构件更稳定。").performScrollTo().assertIsDisplayed()
        assertEquals(1, submissions)
        assertEquals(0, vm.state.value.position)
        snapshot("review-feedback")
        compose.onNodeWithText("下一题").performScrollTo().performClick()
        compose.onNodeWithText("请说明复用的价值。").assertIsDisplayed()
        assertEquals("", vm.state.value.answer)
    }

    @Test fun failedSubmissionKeepsAnswerAndAllowsRetry() {
        var attempts = 0
        val api = fakeApi { name -> when (name) {
            "todayReview" -> ApiResult(200, "ok", listOf(ReviewCard(1, question = "解释缓存穿透？")))
            "submitReview" -> if (++attempts == 1) throw IllegalStateException("网络中断，请重试") else ApiResult(200, "ok", ReviewResult(false, "查询不存在的数据", "可以缓存空结果。"))
            else -> error(name)
        } }
        val vm = ReviewViewModel(api)
        compose.setContent { SecondBrainTheme { ReviewSessionScreen({}, vm) } }
        compose.onNodeWithText("写下你的理解").performTextInput("我的回答")
        compose.onNodeWithText("提交答案").performScrollTo().performClick()
        compose.onNodeWithText("网络中断，请重试").performScrollTo().assertIsDisplayed()
        assertEquals("我的回答", vm.state.value.answer)
        compose.onNodeWithText("提交答案").performScrollTo().performClick()
        compose.onNodeWithText("再巩固一下").performScrollTo().assertIsDisplayed()
        assertEquals(2, attempts)
    }

    @Test fun knowledgeDetailRetriesAndDisplaysFullBody() {
        var calls = 0
        compose.setContent {
            SecondBrainTheme { KnowledgeDetailScreen(7, {}, {
                if (++calls == 1) error("加载失败，请重试")
                KnowledgeNode(7, "在 WSL 中安装 Redis", "在本地搭建可靠的缓存环境", "## 安装 Redis\n\n通过 APT 包管理器安装，并确认服务状态。\n\n```bash\nsudo apt install redis-server\n```\n\n## 验证连接\n\n使用 redis-cli ping 检查连接。")
            }) }
        }
        compose.onNodeWithText("重新加载").performClick()
        compose.onNodeWithText("在 WSL 中安装 Redis").assertIsDisplayed()
        compose.onNodeWithText("使用 redis-cli ping 检查连接。").performScrollTo().assertIsDisplayed()
        snapshot("knowledge-detail")
        assertEquals(2, calls)
    }

    @Test fun shareRendersBodyAndReactionsUseServerState() {
        val post = SquarePost(1, nodeTitle = "高并发场景下锁机制的性能与可靠性权衡", recommendText = "从真实场景理解并发控制。", authorName = "newuser", likeCount = 3, knowledgeNodes = listOf(SharedKnowledgeNode(1, contentMd = "## 理解锁的边界\n\n锁的粒度决定了并发效率，先明确共享资源的边界。")), comments = listOf(SquareComment(1, "user2", "这个例子很清晰。")), commentCount = 1)
        var liked = false
        var bookmarked = false
        compose.setContent { SecondBrainTheme { SquarePostScreen(post, {}, { liked = !liked; liked }, { bookmarked = !bookmarked; bookmarked }, { post }) } }
        compose.onNodeWithText("理解锁的边界").assertIsDisplayed()
        snapshot("share-detail")
        compose.onNodeWithText("点赞 3").performScrollTo().performClick()
        compose.onNodeWithText("已赞 4").assertIsDisplayed()
        compose.onNodeWithText("收藏 0").performClick()
        compose.onNodeWithText("已收藏 1").assertIsDisplayed()
        compose.onNodeWithText("已赞 4").performClick()
        compose.onNodeWithText("点赞 3").assertIsDisplayed()
        compose.onNodeWithText("这个例子很清晰。").performScrollTo().assertIsDisplayed()
    }

    @Test fun workspaceChangeRefreshesLoadedKnowledgeAndReview() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "workspace-flow-${System.nanoTime()}.preferences_pb")
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val session = SessionStore(PreferenceDataStoreFactory.create(scope = scope) { file })
        runBlocking { session.save("personal") }
        val api = fakeApi { name ->
            val token = runBlocking { session.token() }
            when (name) {
                "knowledge" -> ApiResult(200, "ok", KnowledgePage(listOf(KnowledgeNode(1, "$token knowledge"))))
                "todayReview" -> ApiResult(200, "ok", listOf(ReviewCard(1, question = "$token review")))
                "workspaces" -> ApiResult(200, "ok", listOf(Workspace(5, "研发工作区")))
                else -> error(name)
            }
        }
        val knowledge = KnowledgeViewModel(KnowledgeRepository(api), session)
        val today = TodayViewModel(api, session)
        val spaces = WorkspaceViewModel(api, session, context)
        val owners = ViewModelStore().apply { put("knowledge", knowledge); put("today", today); put("spaces", spaces) }
        try {
            compose.setContent { SecondBrainTheme { WorkspaceProfile(PaddingValues(), spaces, {}, {}) } }
            compose.waitUntil(5_000) { (knowledge.state.value as? LoadState.Content)?.value?.firstOrNull()?.title == "personal knowledge" }
            runBlocking { session.save("team", 5) }
            compose.waitUntil(5_000) { (knowledge.state.value as? LoadState.Content)?.value?.firstOrNull()?.title == "team knowledge" && (today.state.value as? LoadState.Content)?.value?.firstOrNull()?.question == "team review" && spaces.selection.value.activeId == 5L }
            snapshot("workspace-selected")
            runBlocking { session.save("personal-again") }
            compose.waitUntil(5_000) { (knowledge.state.value as? LoadState.Content)?.value?.firstOrNull()?.title == "personal-again knowledge" && spaces.selection.value.activeId == null }
            assertNull(runBlocking { session.workspaceId() })
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync { owners.clear() }
            scope.cancel()
        }
    }

    @Test fun communitySwitchesBetweenPopulatedListsWithOverlappingIds() {
        var openedQuestion: Long? = null
        var openedPost: Long? = null
        compose.setContent {
            SecondBrainTheme {
                CommunityContent(PaddingValues(),
                    LoadState.Content(listOf(SquarePost(1, nodeTitle = "分享标题"))),
                    LoadState.Content(listOf(CommunityQuestion(1, "问题标题"))), {}, {},
                    { openedPost = it.postId }, { openedQuestion = it.id })
            }
        }
        repeat(10) {
            compose.onNodeWithText("问答社区").performClick()
            compose.onNodeWithText("问题标题").assertIsDisplayed()
            compose.onNodeWithText("分享标题").assertDoesNotExist()
            compose.onNodeWithText("知识广场").performClick()
            compose.onNodeWithText("分享标题").assertIsDisplayed()
        }
        compose.onNodeWithText("分享标题").performClick()
        assertEquals(1L, openedPost)
        compose.onNodeWithText("问答社区").performClick()
        compose.onNodeWithText("问题标题").performClick()
        assertEquals(1L, openedQuestion)
    }

    @Test fun questionFeedCanRecoverFromLoadingEmptyAndFailureWhileTabsRemainUsable() {
        val questions = mutableStateOf<LoadState<List<CommunityQuestion>>>(LoadState.Loading)
        compose.setContent {
            SecondBrainTheme {
                CommunityContent(PaddingValues(), LoadState.Content(listOf(SquarePost(1, nodeTitle = "分享标题"))),
                    questions.value, {}, { questions.value = LoadState.Content(listOf(CommunityQuestion(1, "恢复后的问题"))) }, {}, {})
            }
        }
        compose.onNodeWithText("问答社区").performClick()
        compose.onNodeWithText("正在读取你的数据…").assertIsDisplayed()
        compose.runOnIdle { questions.value = LoadState.Empty("暂时没有公开问题。") }
        compose.onNodeWithText("暂时没有公开问题。").assertIsDisplayed()
        compose.onNodeWithText("知识广场").performClick()
        compose.onNodeWithText("分享标题").assertIsDisplayed()
        compose.runOnIdle { questions.value = LoadState.Failure("网络连接失败") }
        compose.onNodeWithText("问答社区").performClick()
        compose.onNodeWithText("网络连接失败").assertIsDisplayed()
        compose.onNodeWithText("重新读取").performClick()
        compose.onNodeWithText("恢复后的问题").assertIsDisplayed()
    }

    @Test fun answerRetryPublishesInlineAndSurvivesReentry() {
        var question = CommunityQuestion(1, "如何选择并发锁的粒度？", "希望结合实际场景理解性能与可靠性的取舍。", authorName = "学习伙伴", answers = emptyList())
        var attempts = 0
        val api = fakeApi { name -> when (name) {
            "questionDetail" -> ApiResult(200, "ok", question)
            "answerQuestion" -> {
                if (++attempts == 1) error("连接失败，草稿已保留")
                val answer = CommunityAnswer(12, "测试成员", "先明确共享资源的边界，再选择锁的粒度。")
                question = question.copy(answers = listOf(answer), answerCount = 1)
                ApiResult(200, "ok", answer)
            }
            else -> error(name)
        } }
        val vm = QuestionViewModel(api)
        val visible = mutableStateOf(true)
        compose.setContent { SecondBrainTheme {
            if (visible.value) QuestionScreen(1, { visible.value = false }, vm)
            else androidx.compose.material3.Button(onClick = { visible.value = true }) { androidx.compose.material3.Text("重新进入") }
        } }
        compose.onNode(hasSetTextAction()).performScrollTo().performTextInput("先明确共享资源的边界，再选择锁的粒度。")
        compose.onNodeWithText("发布回答").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.state.value.error != null }
        assertEquals(1, attempts)
        compose.onNodeWithTag("question-content").performScrollToNode(hasText("连接失败，草稿已保留"))
        compose.onNodeWithText("连接失败，草稿已保留").assertIsDisplayed()
        assertTrue(vm.state.value.draft.isNotBlank())
        compose.onNodeWithText("发布回答").performScrollTo().performClick()
        compose.waitUntil(5_000) { vm.state.value.published }
        compose.onNodeWithTag("question-content").performScrollToNode(hasText("回答已发布，可在上方查看"))
        compose.onNodeWithText("回答已发布，可在上方查看").assertIsDisplayed()
        compose.onNodeWithTag("question-content").performScrollToNode(hasText("先明确共享资源的边界，再选择锁的粒度。"))
        compose.onNodeWithText("先明确共享资源的边界，再选择锁的粒度。").assertIsDisplayed()
        snapshot("question-published")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("重新进入").performClick()
        compose.onNodeWithTag("question-content").performScrollToNode(hasText("先明确共享资源的边界，再选择锁的粒度。"))
        compose.onNodeWithText("先明确共享资源的边界，再选择锁的粒度。").assertIsDisplayed()
        assertEquals(2, attempts)
        assertEquals(1, vm.state.value.question?.answerCount)
    }

    private fun snapshot(name: String) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "$name.png")
        compose.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }

    private fun fakeApi(call: (String) -> Any): SecondBrainApi = Proxy.newProxyInstance(
        SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)
    ) { _, method, _ -> call(method.name) } as SecondBrainApi
}
